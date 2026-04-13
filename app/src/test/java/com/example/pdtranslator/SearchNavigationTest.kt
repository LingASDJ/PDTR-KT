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

    assertEquals(1, SearchNavigationOps.findScrollIndex(entries, "weapon"))
  }

  @Test
  fun `returns null when current search result is not on current page`() {
    val entries = listOf(
      translationEntry("magic"),
      translationEntry("weapon")
    )

    assertNull(SearchNavigationOps.findScrollIndex(entries, "armor"))
    assertNull(SearchNavigationOps.findScrollIndex(entries, null))
  }

  @Test
  fun `returns grouped visible index including prefix headers`() {
    val entries = listOf(
      translationEntry("magic.fire"),
      translationEntry("magic.frost"),
      translationEntry("weapon.sword"),
      translationEntry("weapon.axe")
    )

    assertEquals(4, SearchNavigationOps.findGroupedScrollIndex(entries, "weapon.sword"))
    assertEquals(1, SearchNavigationOps.findGroupedScrollIndex(entries, "magic.fire"))
  }

  @Test
  fun `skips entries from collapsed groups before current search result`() {
    val entries = listOf(
      translationEntry("magic.fire"),
      translationEntry("magic.frost"),
      translationEntry("weapon.sword"),
      translationEntry("weapon.axe")
    )

    assertEquals(
      2,
      SearchNavigationOps.findGroupedScrollIndex(
        entries = entries,
        currentSearchResultKey = "weapon.sword",
        collapsedGroups = setOf("magic")
      )
    )
  }

  @Test
  fun `reveals collapsed prefix for current search result`() {
    val entries = listOf(
      translationEntry("magic.fire"),
      translationEntry("weapon.sword")
    )

    assertEquals(
      setOf("magic"),
      SearchNavigationOps.revealGroup(
        collapsedGroups = setOf("magic", "weapon"),
        entries = entries,
        currentSearchResultKey = "weapon.sword"
      )
    )
  }

  @Test
  fun `keeps collapsed groups unchanged when current search result is missing`() {
    val entries = listOf(
      translationEntry("magic.fire"),
      translationEntry("weapon.sword")
    )

    assertEquals(
      setOf("magic", "weapon"),
      SearchNavigationOps.revealGroup(
        collapsedGroups = setOf("magic", "weapon"),
        entries = entries,
        currentSearchResultKey = "armor.shield"
      )
    )
  }

  private fun translationEntry(key: String) = TranslationEntry(
    key = key,
    sourceValue = key,
    targetValue = "",
    originalTargetValue = "",
    isUntranslated = true
  )
}
