package com.example.pdtranslator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineUsagePolicyTest {

  @Test
  fun `never blocks translation based on remembered verification state`() {
    val blockedMessage = EngineUsagePolicy.blockedTranslationMessage(
      healthStatus = EngineHealthStatus(
        state = EngineVerificationState.FAILED,
        message = "Connection timed out"
      ),
      fallbackMessage = "Last verification failed"
    )

    assertNull(blockedMessage)
  }

  @Test
  fun `ignores fallback message when verification is disabled`() {
    val blockedMessage = EngineUsagePolicy.blockedTranslationMessage(
      healthStatus = EngineHealthStatus(
        state = EngineVerificationState.FAILED,
        message = ""
      ),
      fallbackMessage = "Last verification failed"
    )

    assertNull(blockedMessage)
  }

  @Test
  fun `shows base language override whenever an engine is selected`() {
    val verified = EngineHealthStatus(EngineVerificationState.VERIFIED, "ok")
    val failed = EngineHealthStatus(EngineVerificationState.FAILED, "bad key")
    val untested = EngineHealthStatus(EngineVerificationState.UNTESTED, "")

    assertNull(EngineUsagePolicy.blockedTranslationMessage(verified, "fallback"))
    assertNull(EngineUsagePolicy.blockedTranslationMessage(untested, "fallback"))
    assertNull(EngineUsagePolicy.blockedTranslationMessage(failed, "fallback"))
    assertTrue(EngineUsagePolicy.shouldShowBaseLangOverride("google_web", verified))
    assertTrue(EngineUsagePolicy.shouldShowBaseLangOverride("google_web", untested))
    assertTrue(EngineUsagePolicy.shouldShowBaseLangOverride("google_web", failed))
    assertTrue(!EngineUsagePolicy.shouldShowBaseLangOverride("", failed))
  }
}
