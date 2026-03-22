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
}
