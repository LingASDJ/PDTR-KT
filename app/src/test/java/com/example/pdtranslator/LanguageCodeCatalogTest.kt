package com.example.pdtranslator

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageCodeCatalogTest {

  @Test
  fun `filters language options by code and display name`() {
    val options = listOf(
      LanguageCodeOption("zh-CN", "Chinese (Simplified)"),
      LanguageCodeOption("fr", "French"),
      LanguageCodeOption("ja", "Japanese")
    )

    assertEquals(listOf("zh-CN"), LanguageCodeCatalog.filter(options, "zh").map { it.code })
    assertEquals(listOf("fr"), LanguageCodeCatalog.filter(options, "fren").map { it.code })
  }
}
