package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationProgressCalculatorTest {

  @Test
  fun `counts staged edited translations in progress`() {
    val progress = TranslationProgressCalculator.calculate(
      listOf(
        ProgressEntry(sourceValue = "Magic", targetValue = ""),
        ProgressEntry(sourceValue = "Sword", targetValue = "剑"),
        ProgressEntry(sourceValue = "Shield", targetValue = "Shield")
      )
    )

    assertEquals(1f / 3f, progress.ratio, 0.0001f)
    assertEquals(1, progress.translatedCount)
    assertEquals(3, progress.totalCount)
  }
}
