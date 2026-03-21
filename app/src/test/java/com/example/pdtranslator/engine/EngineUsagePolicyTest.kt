package com.example.pdtranslator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineUsagePolicyTest {

  @Test
  fun `blocks translation when last verification failed`() {
    val blockedMessage = EngineUsagePolicy.blockedTranslationMessage(
      healthStatus = EngineHealthStatus(
        state = EngineVerificationState.FAILED,
        message = "Connection timed out"
      ),
      fallbackMessage = "Last verification failed"
    )

    assertEquals("Connection timed out", blockedMessage)
  }

  @Test
  fun `uses fallback message when failed status has no detail`() {
    val blockedMessage = EngineUsagePolicy.blockedTranslationMessage(
      healthStatus = EngineHealthStatus(
        state = EngineVerificationState.FAILED,
        message = ""
      ),
      fallbackMessage = "Last verification failed"
    )

    assertEquals("Last verification failed", blockedMessage)
  }

  @Test
  fun `allows translation and base language override for non failed engines`() {
    val verified = EngineHealthStatus(EngineVerificationState.VERIFIED, "ok")
    val untested = EngineHealthStatus(EngineVerificationState.UNTESTED, "")

    assertNull(EngineUsagePolicy.blockedTranslationMessage(verified, "fallback"))
    assertNull(EngineUsagePolicy.blockedTranslationMessage(untested, "fallback"))
    assertTrue(EngineUsagePolicy.shouldShowBaseLangOverride("google_web", verified))
    assertTrue(EngineUsagePolicy.shouldShowBaseLangOverride("google_web", untested))
  }

  @Test
  fun `hides base language override when engine is missing or failed`() {
    val failed = EngineHealthStatus(EngineVerificationState.FAILED, "bad key")

    assertFalse(EngineUsagePolicy.shouldShowBaseLangOverride("", failed))
    assertFalse(EngineUsagePolicy.shouldShowBaseLangOverride("google_web", failed))
  }
}
