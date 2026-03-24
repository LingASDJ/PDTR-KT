package com.example.pdtranslator.engine

object EngineUsagePolicy {

  fun blockedTranslationMessage(
    healthStatus: EngineHealthStatus,
    fallbackMessage: String
  ): String? {
    return null
  }

  fun shouldShowBaseLangOverride(
    selectedEngineId: String,
    healthStatus: EngineHealthStatus
  ): Boolean {
    return selectedEngineId.isNotBlank()
  }
}
