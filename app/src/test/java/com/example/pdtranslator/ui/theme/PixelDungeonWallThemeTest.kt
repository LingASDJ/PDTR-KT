package com.example.pdtranslator.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelDungeonWallThemeTest {

  @Test
  fun `resolver maps sewers hours to sewers template`() {
    val theme = resolveWallTheme(hour = 4)

    assertEquals(WallChapter.SEWERS, theme.chapter)
    assertTrue(theme.template.moistureBias > 0)
  }

  @Test
  fun `resolver maps city hours to city template`() {
    val theme = resolveWallTheme(hour = 14)

    assertEquals(WallChapter.CITY, theme.chapter)
    assertTrue(theme.template.engravingBias > 0)
  }

  @Test
  fun `resolver applies burning modifier for burning fist`() {
    val theme = resolveWallTheme(hour = 1)

    assertTrue(theme.variant.lavaBoost > 0)
    assertTrue(theme.variant.scorchBoost > 0)
  }

  @Test
  fun `chapters expose strong geometry differences`() {
    val sewers = resolveWallTheme(hour = 4).template
    val prison = resolveWallTheme(hour = 8).template
    val halls = resolveWallTheme(hour = 19).template

    assertTrue(sewers.courseHeightPx != prison.courseHeightPx)
    assertTrue(sewers.mortarThicknessPx > prison.mortarThicknessPx)
    assertTrue(halls.shadowDepthPx > prison.shadowDepthPx)
  }
}
