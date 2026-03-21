package com.example.pdtranslator

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class DictionaryManager(private val context: Context) {

  private val file: File get() = File(context.filesDir, "dictionary.json")
  private val saveMutex = Mutex()
  private val storeState = DictionaryStoreState(DictionaryStore.empty(defaultDictionaryName()))

  private fun newKey(groupName: String, srcLang: String, tgtLang: String, propKey: String): String {
    return "$groupName|$srcLang|$tgtLang|$propKey"
  }

  private fun oldKey(srcLang: String, tgtLang: String, propKey: String): String {
    return "$srcLang|$tgtLang|$propKey"
  }

  private fun globalSuffix(srcLang: String, tgtLang: String, propKey: String): String {
    return "|$srcLang|$tgtLang|$propKey"
  }

  private fun snapshotStore(): DictionaryStore = storeState.snapshot()

  fun getDictionarySummaries(): List<NamedDictionary> {
    return snapshotStore().dictionaries.values.sortedBy { it.name.lowercase() }
  }

  fun getSelectedDictionaryId(): String = snapshotStore().selectedDictionaryId

  fun getSelectedDictionaryName(): String = snapshotStore().selectedDictionary.name

  fun getDictionaryCount(): Int = snapshotStore().dictionaries.size

  fun canDeleteSelectedDictionary(): Boolean = snapshotStore().dictionaries.size > 1

  fun selectDictionary(id: String) {
    storeState.update { store ->
      store.selectDictionary(id).normalized(defaultDictionaryName())
    }
  }

  fun createDictionary(name: String) {
    val trimmed = name.trim()
    require(trimmed.isNotBlank()) { "blank_name" }
    val store = snapshotStore()
    require(store.dictionaries.values.none { it.name.equals(trimmed, ignoreCase = true) }) { "duplicate_name" }
    storeState.update { current ->
      current.createDictionary(trimmed).normalized(defaultDictionaryName())
    }
  }

  fun renameSelectedDictionary(name: String) {
    val trimmed = name.trim()
    require(trimmed.isNotBlank()) { "blank_name" }
    val store = snapshotStore()
    require(
      store.dictionaries.values.none {
        it.id != store.selectedDictionaryId && it.name.equals(trimmed, ignoreCase = true)
      }
    ) { "duplicate_name" }
    storeState.update { current ->
      current.renameDictionary(selectedOnly = true, newName = trimmed).normalized(defaultDictionaryName())
    }
  }

  fun deleteSelectedDictionary(): Boolean {
    if (!canDeleteSelectedDictionary()) return false
    storeState.update { store ->
      store.deleteDictionary(store.selectedDictionaryId).normalized(defaultDictionaryName())
    }
    return true
  }

  private fun selectedEntries(): Map<String, DictEntry> = snapshotStore().selectedDictionary.entries

  fun addEntry(groupName: String, srcLang: String, tgtLang: String, propKey: String, sourceText: String, translation: String) {
    if (translation.isBlank()) return
    storeState.update { store ->
      val nk = newKey(groupName, srcLang, tgtLang, propKey)
      val ok = oldKey(srcLang, tgtLang, propKey)
      val cleanedEntries = LinkedHashMap(store.selectedDictionary.entries)
      cleanedEntries.remove(ok)
      cleanedEntries[nk] = DictEntry(
        sourceText = sourceText,
        translation = translation,
        timestamp = System.currentTimeMillis()
      )
      val updatedDictionary = store.selectedDictionary.copy(entries = cleanedEntries)
      store.copy(
        dictionaries = LinkedHashMap(store.dictionaries).apply { put(updatedDictionary.id, updatedDictionary) }
      ).normalized(defaultDictionaryName())
    }
  }

  fun getEntry(groupName: String, srcLang: String, tgtLang: String, propKey: String): DictEntry? {
    val entries = selectedEntries()
    val nk = newKey(groupName, srcLang, tgtLang, propKey)
    entries[nk]?.let { return it }
    val ok = oldKey(srcLang, tgtLang, propKey)
    entries[ok]?.let { return it }
    val suffix = globalSuffix(srcLang, tgtLang, propKey)
    return entries.entries
      .filter { it.key.endsWith(suffix) && it.key.count { c -> c == '|' } == 3 }
      .maxByOrNull { it.value.timestamp }
      ?.value
  }

  fun getTranslation(groupName: String, srcLang: String, tgtLang: String, propKey: String): String? {
    return getEntry(groupName, srcLang, tgtLang, propKey)?.translation
  }

  fun getTotalCount(): Int = snapshotStore().selectedDictionary.entryCount

  fun importFromProperties(
    sourceProps: java.util.Properties,
    targetProps: java.util.Properties,
    groupName: String,
    srcLang: String,
    tgtLang: String
  ): Int {
    var count = 0
    sourceProps.forEach { (key, srcValue) ->
      val propKey = key as String
      val sourceText = srcValue as String
      val targetText = targetProps.getProperty(propKey)
      if (!targetText.isNullOrBlank() && sourceText.isNotBlank()) {
        addEntry(groupName, srcLang, tgtLang, propKey, sourceText, targetText)
        count++
      }
    }
    return count
  }

  fun applyToEntries(
    entries: List<TranslationEntry>,
    groupName: String,
    srcLang: String,
    tgtLang: String
  ): Map<String, String> {
    val applied = mutableMapOf<String, String>()
    entries.forEach { entry ->
      if (entry.targetValue.isBlank() || entry.isMissing) {
        val dictEntry = getEntry(groupName, srcLang, tgtLang, entry.key)
        if (dictEntry != null &&
          (dictEntry.sourceText == null || dictEntry.sourceText == entry.sourceValue)
        ) {
          applied[entry.key] = dictEntry.translation
        }
      }
    }
    return applied
  }

  suspend fun save() {
    saveMutex.withLock {
      val snapshot = snapshotStore()
      withContext(Dispatchers.IO) {
        val json = DictionaryStoreSerializer.toJson(snapshot)
        writeTextAtomically(file, json)
      }
    }
  }

  suspend fun load() {
    val loadedStore = withContext(Dispatchers.IO) {
      if (file.exists()) {
        try {
          DictionaryStoreSerializer.fromJson(file.readText(Charsets.UTF_8), defaultDictionaryName())
        } catch (_: Exception) {
          DictionaryStore.empty(defaultDictionaryName())
        }
      } else {
        DictionaryStore.empty(defaultDictionaryName())
      }
    }
    storeState.replace(loadedStore)
  }

  suspend fun clear() {
    storeState.update { store ->
      store.clearDictionary().normalized(defaultDictionaryName())
    }
    save()
  }

  private fun defaultDictionaryName(): String {
    return runCatching { context.getString(R.string.dict_default_name) }.getOrDefault(DictionaryStore.DEFAULT_NAME)
  }
}
