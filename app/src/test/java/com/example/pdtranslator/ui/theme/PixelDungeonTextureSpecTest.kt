package com.example.pdtranslator.ui.theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelDungeonTextureSpecTest {

  @Test
  fun `brick texture uses capped dimensions instead of full screen size`() {
    val spec = PixelDungeonWallDebugApi.brickTextureSpec(screenWidthPx = 1440, screenHeightPx = 3200)

    assertTrue(spec.textureWidthPx < 1440)
    assertTrue(spec.textureHeightPx < 3200)
    assertTrue(spec.textureWidthPx > 0)
    assertTrue(spec.textureHeightPx > 0)
  }

  @Test
  fun `brick rows use varied offsets so layout is not fixed`() {
    val offsets = (0 until 16).map { row -> PixelDungeonWallDebugApi.brickRowOffset(row = row, hour = 8) }.toSet()

    assertTrue(offsets.size > 1)
  }

  @Test
  fun `brick layout changes when startup seed changes`() {
    val first = (0 until 16).map { row -> PixelDungeonWallDebugApi.brickRowOffset(row = row, hour = 8, layoutSeed = 11) }
    val second = (0 until 16).map { row -> PixelDungeonWallDebugApi.brickRowOffset(row = row, hour = 8, layoutSeed = 97) }

    assertTrue(first.zip(second).any { (a, b) -> a != b })
  }

  @Test
  fun `different chapters produce different row structure`() {
    val sewersTheme = PixelDungeonWallDebugApi.resolveWallTheme(hour = 4)
    val prisonTheme = PixelDungeonWallDebugApi.resolveWallTheme(hour = 8)

    val sewersPlan = PixelDungeonWallDebugApi.brickRowPlan(row = 3, hour = 4, theme = sewersTheme, layoutSeed = 11)
    val prisonPlan = PixelDungeonWallDebugApi.brickRowPlan(row = 3, hour = 8, theme = prisonTheme, layoutSeed = 11)

    assertTrue(sewersPlan.segmentWidths != prisonPlan.segmentWidths)
  }

  @Test
  fun `same seed stays stable while different seed changes row output`() {
    val theme = PixelDungeonWallDebugApi.resolveWallTheme(hour = 19)

    val first = PixelDungeonWallDebugApi.brickRowPlan(row = 5, hour = 19, theme = theme, layoutSeed = 21)
    val second = PixelDungeonWallDebugApi.brickRowPlan(row = 5, hour = 19, theme = theme, layoutSeed = 21)
    val third = PixelDungeonWallDebugApi.brickRowPlan(row = 5, hour = 19, theme = theme, layoutSeed = 77)

    assertEquals(first, second)
    assertTrue(first != third)
  }

  @Test
  fun `row plans carry chapter geometry and not only widths`() {
    val sewersPlan = PixelDungeonWallDebugApi.brickRowPlan(row = 2, hour = 4, theme = PixelDungeonWallDebugApi.resolveWallTheme(hour = 4), layoutSeed = 19)
    val cityPlan = PixelDungeonWallDebugApi.brickRowPlan(row = 2, hour = 14, theme = PixelDungeonWallDebugApi.resolveWallTheme(hour = 14), layoutSeed = 19)

    assertTrue(sewersPlan.courseHeightPx != cityPlan.courseHeightPx || sewersPlan.mortarThicknessPx != cityPlan.mortarThicknessPx)
    assertTrue(sewersPlan.shadowDepthPx != cityPlan.shadowDepthPx)
  }
}
