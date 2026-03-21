package com.example.pdtranslator.engine

data class TranslationResult(
  val translatedText: String,
  val engineName: String
)

data class EngineConfig(
  val id: String,
  val nameResId: Int,
  val isExperimental: Boolean = false,
  val requiresApiKey: Boolean = true,
  val requiresEndpoint: Boolean = false
)

interface TranslationEngine {
  val config: EngineConfig

  suspend fun translate(
    text: String,
    sourceLang: String,
    targetLang: String
  ): Result<TranslationResult>

  suspend fun translateBatch(
    texts: List<String>,
    sourceLang: String,
    targetLang: String
  ): Result<List<TranslationResult>> {
    // Default: translate one by one
    val results = mutableListOf<TranslationResult>()
    for (t in texts) {
      val r = translate(t, sourceLang, targetLang)
      if (r.isFailure) return Result.failure(r.exceptionOrNull() ?: Exception("Translation failed"))
      results.add(r.getOrThrow())
    }
    return Result.success(results)
  }

  suspend fun testConnection(): Result<String>
}
