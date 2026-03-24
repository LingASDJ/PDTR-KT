package com.example.pdtranslator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoudaoWebLanguagePolicyTest {

  @Test
  fun `builds retry candidates for traditional chinese source`() {
    val attempts = YoudaoWebLanguagePolicy.buildAttemptPairs("zh-TW", "en")

    assertEquals(
      listOf(
        LangPair("zh-CHT", "en"),
        LangPair("zh-CHS", "en"),
        LangPair("auto", "en")
      ),
      attempts
    )
  }

  @Test
  fun `adds auto fallback for non auto source requests`() {
    val attempts = YoudaoWebLanguagePolicy.buildAttemptPairs("ja", "zh-CN")

    assertEquals(
      listOf(
        LangPair("ja", "zh-CHS"),
        LangPair("auto", "zh-CHS")
      ),
      attempts
    )
  }

  @Test
  fun `describes unsupported pair failures clearly`() {
    val message = YoudaoWebLanguagePolicy.buildUnsupportedMessage("ja", "zh-CN")

    assertTrue(message.contains("Youdao Web"))
    assertTrue(message.contains("ja"))
    assertTrue(message.contains("zh-CN"))
  }

  @Test
  fun `detects error code 50 responses`() {
    assertTrue(YoudaoWebLanguagePolicy.isErrorCode50("Youdao webtranslate error code: 50"))
  }
}
