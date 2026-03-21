package com.example.pdtranslator.engine

object EngineUsagePolicy {

  fun blockedTranslationMessage(
    healthStatus: EngineHealthStatus,
    fallbackMessage: String
  ): String? {
    if (healthStatus.state != EngineVerificationState.FAILED) return null
    return healthStatus.message.ifBlank { fallbackMessage }
  }

  fun shouldShowBaseLangOverride(
    selectedEngineId: String,
    healthStatus: EngineHealthStatus
  ): Boolean {
    return selectedEngineId.isNotBlank() && healthStatus.state != EngineVerificationState.FAILED
  }
}
