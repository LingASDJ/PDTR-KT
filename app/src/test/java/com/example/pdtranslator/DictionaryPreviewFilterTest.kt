package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryPreviewFilterTest {

  @Test
  fun `filters preview entries by key source text and translation`() {
    val dictionary = NamedDictionary(
      id = "shared",
      name = "Shared",
      entries = linkedMapOf(
        "item.magic" to DictEntry(sourceText = "Magic Wand", translation = "魔杖", timestamp = 1L),
        "boss.king" to DictEntry(sourceText = "King of Dwarves", translation = "矮人国王", timestamp = 2L)
      )
    )

    assertEquals(listOf("item.magic"), DictionaryPreviewFilter.filter(dictionary, "item").map { it.rawKey })
    assertEquals(listOf("item.magic"), DictionaryPreviewFilter.filter(dictionary, "wand").map { it.rawKey })
    assertEquals(listOf("boss.king"), DictionaryPreviewFilter.filter(dictionary, "矮人").map { it.rawKey })
  }
}
