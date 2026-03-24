package com.example.pdtranslator

import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class DictionaryImportResult(
  val store: DictionaryStore,
  val importedCount: Int
)

data class DictionaryImportPreview(
  val importedCount: Int,
  val conflictNames: List<String>
)

data class DictionaryIndexDocument(
  val selectedDictionaryId: String,
  val dictionaryIds: List<String>
)

data class DictionaryDocument(
  val id: String,
  val name: String,
  val entries: Map<String, DictEntry> = emptyMap()
)

class DictionaryRepository(
  private val rootDir: File,
  private val defaultName: String = DictionaryStore.DEFAULT_NAME
) {
  private val gson = Gson()

  private data class ParsedImportPayload(
    val documents: List<DictionaryDocument>,
    val selectedImportedId: String?
  )

  fun load(legacyFile: File? = null): DictionaryStore {
    val indexFile = File(rootDir, INDEX_FILE_NAME)
    if (indexFile.exists()) {
      return loadFromRepository()
    }

    if (legacyFile != null && legacyFile.exists()) {
      val migrated = DictionaryStoreSerializer
        .fromJson(legacyFile.readText(Charsets.UTF_8), defaultName)
        .normalized(defaultName)
      save(migrated)
      legacyFile.delete()
      return migrated
    }

    return DictionaryStore.empty(defaultName)
  }

  fun save(store: DictionaryStore) {
    val normalized = store.normalized(defaultName)
    rootDir.mkdirs()

    val indexDocument = DictionaryIndexDocument(
      selectedDictionaryId = normalized.selectedDictionaryId,
      dictionaryIds = normalized.dictionaries.keys.toList()
    )
    writeTextAtomically(File(rootDir, INDEX_FILE_NAME), gson.toJson(indexDocument))

    normalized.dictionaries.values.forEach { dictionary ->
      val document = DictionaryDocument(
        id = dictionary.id,
        name = dictionary.name,
        entries = LinkedHashMap(dictionary.entries)
      )
      writeTextAtomically(
        File(rootDir, dictionaryFileName(dictionary.id)),
        gson.toJson(document)
      )
    }

    val expectedFiles = normalized.dictionaries.keys.mapTo(mutableSetOf()) { dictionaryFileName(it) }
    rootDir.listFiles()
      ?.filter { it.isFile && it.name.startsWith(DICTIONARY_FILE_PREFIX) && it.name.endsWith(".json") }
      ?.forEach { file ->
        if (file.name !in expectedFiles) {
          file.delete()
        }
      }
  }

  fun exportSelected(store: DictionaryStore, dictionaryId: String = store.selectedDictionaryId): String {
    val normalized = store.normalized(defaultName)
    val dictionary = normalized.dictionaries[dictionaryId] ?: normalized.selectedDictionary
    return gson.toJson(
      DictionaryDocument(
        id = dictionary.id,
        name = dictionary.name,
        entries = LinkedHashMap(dictionary.entries)
      )
    )
  }

  fun exportAll(store: DictionaryStore): ByteArray {
    val normalized = store.normalized(defaultName)
    return ByteArrayOutputStream().use { output ->
      ZipOutputStream(output).use { zip ->
        val indexDocument = DictionaryIndexDocument(
          selectedDictionaryId = normalized.selectedDictionaryId,
          dictionaryIds = normalized.dictionaries.keys.toList()
        )
        writeZipEntry(zip, INDEX_FILE_NAME, gson.toJson(indexDocument))
        normalized.dictionaries.values.forEach { dictionary ->
          writeZipEntry(
            zip,
            dictionaryFileName(dictionary.id),
            gson.toJson(
              DictionaryDocument(
                id = dictionary.id,
                name = dictionary.name,
                entries = LinkedHashMap(dictionary.entries)
              )
            )
          )
        }
      }
      output.toByteArray()
    }
  }

  fun importSingle(store: DictionaryStore, json: String): DictionaryImportResult {
    val document = parseDictionaryDocument(json)
    return mergeDocuments(store, listOf(document), document.id)
  }

  fun previewSingleImport(store: DictionaryStore, json: String): DictionaryImportPreview {
    return previewDocuments(store, listOf(parseDictionaryDocument(json)))
  }

  fun importArchive(store: DictionaryStore, zipBytes: ByteArray): DictionaryImportResult {
    val payload = parseArchiveImport(zipBytes)
    return mergeDocuments(store, payload.documents, payload.selectedImportedId)
  }

  fun previewArchiveImport(store: DictionaryStore, zipBytes: ByteArray): DictionaryImportPreview {
    return previewDocuments(store, parseArchiveImport(zipBytes).documents)
  }

  private fun loadFromRepository(): DictionaryStore {
    val indexFile = File(rootDir, INDEX_FILE_NAME)
    val index = gson.fromJson(
      indexFile.readText(Charsets.UTF_8),
      DictionaryIndexDocument::class.java
    ) ?: return DictionaryStore.empty(defaultName)

    val dictionaries = LinkedHashMap<String, NamedDictionary>()
    index.dictionaryIds.forEach { dictionaryId ->
      val dictionaryFile = File(rootDir, dictionaryFileName(dictionaryId))
      if (!dictionaryFile.exists()) return@forEach
      val document = parseDictionaryDocument(dictionaryFile.readText(Charsets.UTF_8))
      dictionaries[document.id] = NamedDictionary(
        id = document.id,
        name = document.name.ifBlank { defaultName },
        entries = LinkedHashMap(document.entries)
      )
    }

    return DictionaryStore(
      selectedDictionaryId = index.selectedDictionaryId,
      dictionaries = dictionaries
    ).normalized(defaultName)
  }

  private fun mergeDocuments(
    store: DictionaryStore,
    documents: List<DictionaryDocument>,
    selectedImportedId: String?
  ): DictionaryImportResult {
    var current = store.normalized(defaultName)
    val existingIds = current.dictionaries.keys.toMutableSet()
    val existingNames = current.dictionaries.values.map { it.name.lowercase(Locale.ROOT) }.toMutableSet()
    val importedIdMap = mutableMapOf<String, String>()

    documents.forEach { document ->
      val normalizedName = document.name.trim().ifBlank { defaultName }
      val normalizedId = uniqueId(
        candidate = document.id.trim().ifBlank { slugify(normalizedName) },
        usedIds = existingIds
      )
      val uniqueName = uniqueName(normalizedName, existingNames)
      val importedDictionary = NamedDictionary(
        id = normalizedId,
        name = uniqueName,
        entries = LinkedHashMap(document.entries)
      )
      val updatedDictionaries = LinkedHashMap(current.dictionaries)
      updatedDictionaries[normalizedId] = importedDictionary
      current = current.copy(dictionaries = updatedDictionaries).normalized(defaultName)
      existingIds += normalizedId
      existingNames += uniqueName.lowercase(Locale.ROOT)
      importedIdMap[document.id] = normalizedId
    }

    val selectedId = selectedImportedId
      ?.let { importedIdMap[it] }
      ?: importedIdMap.values.lastOrNull()
      ?: current.selectedDictionaryId

    return DictionaryImportResult(
      store = current.selectDictionary(selectedId).normalized(defaultName),
      importedCount = documents.size
    )
  }

  private fun previewDocuments(
    store: DictionaryStore,
    documents: List<DictionaryDocument>
  ): DictionaryImportPreview {
    val existingNames = store.normalized(defaultName)
      .dictionaries
      .values
      .map { it.name.lowercase(Locale.ROOT) }
      .toMutableSet()
    val conflictNames = mutableListOf<String>()

    documents.forEach { document ->
      val normalizedName = document.name.trim().ifBlank { defaultName }
      val normalizedKey = normalizedName.lowercase(Locale.ROOT)
      if (normalizedKey in existingNames) {
        conflictNames += normalizedName
      }
      existingNames += normalizedKey
    }

    return DictionaryImportPreview(
      importedCount = documents.size,
      conflictNames = conflictNames.distinct()
    )
  }

  private fun parseArchiveImport(zipBytes: ByteArray): ParsedImportPayload {
    val entries = mutableMapOf<String, String>()
    ZipInputStream(zipBytes.inputStream()).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          entries[entry.name.substringAfterLast('/')] = zip.readBytes().toString(Charsets.UTF_8)
        }
        entry = zip.nextEntry
      }
    }

    val indexJson = entries[INDEX_FILE_NAME]
      ?: throw IllegalArgumentException("missing_index")
    val index = gson.fromJson(indexJson, DictionaryIndexDocument::class.java)
      ?: throw IllegalArgumentException("invalid_index")
    val documents = index.dictionaryIds.mapNotNull { dictionaryId ->
      entries[dictionaryFileName(dictionaryId)]?.let(::parseDictionaryDocument)
    }
    if (documents.isEmpty()) {
      throw IllegalArgumentException("missing_dictionaries")
    }
    return ParsedImportPayload(
      documents = documents,
      selectedImportedId = index.selectedDictionaryId
    )
  }

  private fun parseDictionaryDocument(json: String): DictionaryDocument {
    val document = gson.fromJson(json, DictionaryDocument::class.java)
      ?: throw IllegalArgumentException("invalid_dictionary")
    val normalizedName = document.name.trim().ifBlank { defaultName }
    val normalizedId = document.id.trim().ifBlank { slugify(normalizedName) }
    return DictionaryDocument(
      id = normalizedId,
      name = normalizedName,
      entries = LinkedHashMap(document.entries)
    )
  }

  private fun uniqueId(candidate: String, usedIds: Set<String>): String {
    val sanitizedBase = slugify(candidate)
    if (sanitizedBase !in usedIds) return sanitizedBase
    var suffix = 1
    var nextId = "$sanitizedBase-$suffix"
    while (nextId in usedIds) {
      suffix += 1
      nextId = "$sanitizedBase-$suffix"
    }
    return nextId
  }

  private fun uniqueName(candidate: String, usedNames: Set<String>): String {
    if (candidate.lowercase(Locale.ROOT) !in usedNames) return candidate
    var suffix = 1
    var nextName = "$candidate ($suffix)"
    while (nextName.lowercase(Locale.ROOT) in usedNames) {
      suffix += 1
      nextName = "$candidate ($suffix)"
    }
    return nextName
  }

  private fun slugify(value: String): String {
    val base = value
      .lowercase(Locale.ROOT)
      .replace(Regex("[^a-z0-9]+"), "-")
      .trim('-')
    return if (base.isBlank()) "dictionary" else base
  }

  private fun writeZipEntry(zip: ZipOutputStream, name: String, text: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(text.toByteArray(Charsets.UTF_8))
    zip.closeEntry()
  }

  companion object {
    const val INDEX_FILE_NAME = "index.json"
    private const val DICTIONARY_FILE_PREFIX = "dict-"

    fun dictionaryFileName(id: String): String = "$DICTIONARY_FILE_PREFIX$id.json"
  }
}
