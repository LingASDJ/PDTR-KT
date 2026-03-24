package com.example.pdtranslator.engine

data class MicrosoftCredentials(
  val apiKey: String,
  val region: String? = null
) {
  companion object {
    fun parse(raw: String): MicrosoftCredentials {
      val parts = raw.split("|", limit = 2)
      val apiKey = parts.firstOrNull().orEmpty().trim()
      val region = parts.getOrNull(1)?.trim().orEmpty().ifBlank { null }
      return MicrosoftCredentials(apiKey = apiKey, region = region)
    }
  }
}
