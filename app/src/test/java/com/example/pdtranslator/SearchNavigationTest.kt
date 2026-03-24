package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchNavigationTest {

  @Test
  fun `returns visible index for current search result key`() {
    val entries = listOf(
      translationEntry("magic"),
      translationEntry("weapon"),
      translationEntry("armor")
    )

    assertEquals(1, findSearchResultScrollIndex(entries, "weapon"))
  }

  @Test
  fun `returns null when current search result is not on current page`() {
    val entries = listOf(
      translationEntry("magic"),
      translationEntry("weapon")
    )

    assertNull(findSearchResultScrollIndex(entries, "armor"))
    assertNull(findSearchResultScrollIndex(entries, null))
  }

  private fun translationEntry(key: String) = TranslationEntry(
    key = key,
    sourceValue = key,
    targetValue = "",
    originalTargetValue = "",
    isUntranslated = true
  )
}
