package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream

class DictionaryRepositoryTest {

  @Test
  fun `saves each dictionary as separate file and loads round trip`() {
    val dir = createTempDir(prefix = "dict-repo-roundtrip")
    try {
      val repository = DictionaryRepository(File(dir, "dictionaries"))
      val store = DictionaryStore.empty()
        .putEntry(
          "items|base|zh-CN|item.magic",
          DictEntry(sourceText = "Magic", translation = "魔法", timestamp = 1L)
        )
        .createDictionary("Boss Terms")
        .putEntry(
          "bosses|base|zh-CN|boss.king",
          DictEntry(sourceText = "King", translation = "国王", timestamp = 2L)
        )

      repository.save(store)

      assertTrue(File(dir, "dictionaries/index.json").exists())
      assertTrue(File(dir, "dictionaries/${DictionaryRepository.dictionaryFileName(DictionaryStore.DEFAULT_ID)}").exists())
      assertTrue(File(dir, "dictionaries/${DictionaryRepository.dictionaryFileName("boss-terms")}").exists())

      val loaded = repository.load()

      assertEquals(store.selectedDictionaryId, loaded.selectedDictionaryId)
      assertEquals(2, loaded.dictionaries.size)
      assertEquals("国王", loaded.selectedDictionary.entries["bosses|base|zh-CN|boss.king"]?.translation)
      assertEquals("魔法", loaded.dictionaries[DictionaryStore.DEFAULT_ID]?.entries?.get("items|base|zh-CN|item.magic")?.translation)
    } finally {
      dir.deleteRecursively()
    }
  }

  @Test
  fun `migrates legacy dictionary json into repository directory`() {
    val dir = createTempDir(prefix = "dict-repo-migrate")
    try {
      val repository = DictionaryRepository(File(dir, "dictionaries"))
      val legacyFile = File(dir, "dictionary.json").apply {
        writeText(
          """
          {
            "items|base|zh-CN|item.magic": {
              "sourceText": "Magic",
              "translation": "魔法",
              "timestamp": 1
            }
          }
          """.trimIndent(),
          Charsets.UTF_8
        )
      }

      val loaded = repository.load(legacyFile = legacyFile)

      assertFalse(legacyFile.exists())
      assertTrue(File(dir, "dictionaries/index.json").exists())
      assertEquals(1, loaded.dictionaries.size)
      assertEquals(1, loaded.selectedDictionary.entryCount)
      assertEquals("魔法", loaded.selectedDictionary.entries.values.first().translation)
    } finally {
      dir.deleteRecursively()
    }
  }

  @Test
  fun `exports selected dictionary as standalone document and imports with unique name suffix`() {
    val dir = createTempDir(prefix = "dict-repo-single")
    try {
      val repository = DictionaryRepository(File(dir, "dictionaries"))
      val sourceStore = DictionaryStore.empty()
        .renameDictionary(selectedOnly = true, newName = "Shared Terms")
        .putEntry(
          "items|base|zh-CN|item.magic",
          DictEntry(sourceText = "Magic", translation = "魔法", timestamp = 1L)
        )
      val targetStore = DictionaryStore.empty()
        .renameDictionary(selectedOnly = true, newName = "Shared Terms")

      val exported = repository.exportSelected(sourceStore)
      val imported = repository.importSingle(targetStore, exported)

      assertEquals(2, imported.store.dictionaries.size)
      assertEquals(1, imported.importedCount)
      assertEquals("Shared Terms (1)", imported.store.selectedDictionary.name)
      assertEquals("魔法", imported.store.selectedDictionary.entries.values.first().translation)
    } finally {
      dir.deleteRecursively()
    }
  }

  @Test
  fun `previews single dictionary import conflicts before applying rename all`() {
    val dir = createTempDir(prefix = "dict-repo-preview-single")
    try {
      val repository = DictionaryRepository(File(dir, "dictionaries"))
      val sourceStore = DictionaryStore.empty()
        .renameDictionary(selectedOnly = true, newName = "Shared Terms")
        .putEntry(
          "items|base|zh-CN|item.magic",
          DictEntry(sourceText = "Magic", translation = "魔法", timestamp = 1L)
        )
      val targetStore = DictionaryStore.empty()
        .renameDictionary(selectedOnly = true, newName = "Shared Terms")

      val preview = repository.previewSingleImport(targetStore, repository.exportSelected(sourceStore))

      assertEquals(1, preview.importedCount)
      assertEquals(listOf("Shared Terms"), preview.conflictNames)
    } finally {
      dir.deleteRecursively()
    }
  }

  @Test
  fun `exports all dictionaries as zip package and imports preserving selected dictionary`() {
    val dir = createTempDir(prefix = "dict-repo-archive")
    try {
      val repository = DictionaryRepository(File(dir, "dictionaries"))
      val store = DictionaryStore.empty()
        .renameDictionary(selectedOnly = true, newName = "Core Terms")
        .createDictionary("Boss Terms")
        .putEntry(
          "bosses|base|zh-CN|boss.king",
          DictEntry(sourceText = "King", translation = "国王", timestamp = 2L)
        )

      val archiveBytes = repository.exportAll(store)
      val entryNames = ZipInputStream(archiveBytes.inputStream()).use { zis ->
        buildList {
          var entry = zis.nextEntry
          while (entry != null) {
            add(entry.name)
            entry = zis.nextEntry
          }
        }
      }

      assertTrue("index.json" in entryNames)
      assertTrue(DictionaryRepository.dictionaryFileName(DictionaryStore.DEFAULT_ID) in entryNames)
      assertTrue(DictionaryRepository.dictionaryFileName("boss-terms") in entryNames)

      val imported = repository.importArchive(DictionaryStore.empty(), archiveBytes)

      assertEquals(2, imported.importedCount)
      assertEquals("Boss Terms", imported.store.selectedDictionary.name)
      assertEquals("国王", imported.store.selectedDictionary.entries.values.first().translation)
    } finally {
      dir.deleteRecursively()
    }
  }

  @Test
  fun `previews archive import conflicts before applying rename all`() {
    val dir = createTempDir(prefix = "dict-repo-preview-archive")
    try {
      val repository = DictionaryRepository(File(dir, "dictionaries"))
      val sourceStore = DictionaryStore.empty()
        .renameDictionary(selectedOnly = true, newName = "Core Terms")
        .createDictionary("Boss Terms")
        .putEntry(
          "bosses|base|zh-CN|boss.king",
          DictEntry(sourceText = "King", translation = "国王", timestamp = 2L)
        )
      val targetStore = DictionaryStore.empty()
        .createDictionary("Boss Terms")

      val preview = repository.previewArchiveImport(targetStore, repository.exportAll(sourceStore))

      assertEquals(2, preview.importedCount)
      assertEquals(listOf("Boss Terms"), preview.conflictNames)
    } finally {
      dir.deleteRecursively()
    }
  }
}
