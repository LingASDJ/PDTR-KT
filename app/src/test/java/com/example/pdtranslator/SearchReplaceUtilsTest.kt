package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchReplaceUtilsTest {

  @Test
  fun `matches query against key source and target ignoring case by default`() {
    val entry = SearchableEntry(
      key = "item.magicWand",
      sourceValue = "Magic Wand",
      targetValue = "魔法棒"
    )

    assertTrue(SearchReplaceUtils.matches(entry, "magic", caseSensitive = false, exactMatch = false))
    assertTrue(SearchReplaceUtils.matches(entry, "魔法", caseSensitive = false, exactMatch = false))
    assertFalse(SearchReplaceUtils.matches(entry, "sword", caseSensitive = false, exactMatch = false))
  }

  @Test
  fun `replace returns updated text for case insensitive partial matches`() {
    val replaced = SearchReplaceUtils.replaceTarget(
      original = "Magic magic MAGic",
      search = "magic",
      replacement = "Arcane",
      caseSensitive = false,
      exactMatch = false
    )

    assertEquals("Arcane Arcane Arcane", replaced)
  }

  @Test
  fun `replace exact match swaps whole value only when identical`() {
    val replaced = SearchReplaceUtils.replaceTarget(
      original = "Magic",
      search = "Magic",
      replacement = "Arcane",
      caseSensitive = true,
      exactMatch = true
    )

    assertEquals("Arcane", replaced)
  }
}
