package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryStoreTest {

  @Test
  fun `migrates legacy flat dictionary json into default named dictionary`() {
    val legacyJson = """
      {
        "items|base|zh-CN|item.magic": {
          "sourceText": "Magic",
          "translation": "魔法",
          "timestamp": 1
        }
      }
    """.trimIndent()

    val store = DictionaryStoreSerializer.fromJson(legacyJson)

    assertEquals(1, store.dictionaries.size)
    assertEquals(store.selectedDictionaryId, store.dictionaries.keys.first())
    assertEquals(1, store.selectedDictionary.entryCount)
    assertEquals("魔法", store.selectedDictionary.entries.values.first().translation)
  }

  @Test
  fun `supports create select rename and delete dictionary`() {
    val created = DictionaryStore.empty()
      .createDictionary("Boss Terms")
      .renameDictionary(selectedOnly = true, newName = "Boss Terminology")

    assertEquals("Boss Terminology", created.selectedDictionary.name)
    assertEquals(2, created.dictionaries.size)

    val selectedDefault = created.selectDictionary(created.dictionaries.values.first { it.name != "Boss Terminology" }.id)
    val deleted = selectedDefault.deleteDictionary(created.selectedDictionaryId)

    assertEquals(1, deleted.dictionaries.size)
    assertTrue(deleted.selectedDictionary.name.isNotBlank())
  }

  @Test
  fun `updates existing dictionary entry without touching unrelated entries`() {
    val store = DictionaryStore.empty()
      .putEntry(
        entryKey = "actors|base|zh-CN|actor.hero",
        value = DictEntry(sourceText = "Hero", translation = "英雄", timestamp = 1L)
      )
      .putEntry(
        entryKey = "items|base|zh-CN|item.wand",
        value = DictEntry(sourceText = "Wand", translation = "法杖", timestamp = 2L)
      )

    val updated = store.updateEntry(
      entryKey = "actors|base|zh-CN|actor.hero",
      sourceText = "The Hero",
      translation = "主角"
    )

    assertEquals("The Hero", updated.selectedDictionary.entries["actors|base|zh-CN|actor.hero"]?.sourceText)
    assertEquals("主角", updated.selectedDictionary.entries["actors|base|zh-CN|actor.hero"]?.translation)
    assertEquals("法杖", updated.selectedDictionary.entries["items|base|zh-CN|item.wand"]?.translation)
  }
}
