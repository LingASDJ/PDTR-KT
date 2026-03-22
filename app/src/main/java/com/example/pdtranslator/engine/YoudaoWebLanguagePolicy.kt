package com.example.pdtranslator.engine

data class LangPair(val from: String, val to: String)

object YoudaoWebLanguagePolicy {

  fun mapLang(lang: String): String {
    val normalized = lang.trim()
    return when {
      normalized.equals("base", ignoreCase = true) -> "en"
      normalized.equals("auto", ignoreCase = true) -> "auto"
      normalized.startsWith("zh", ignoreCase = true) -> {
        if (isTraditionalChineseVariant(normalized)) "zh-CHT" else "zh-CHS"
      }
      else -> normalized.substringBefore("-").lowercase()
    }
  }

  fun buildAttemptPairs(sourceLang: String, targetLang: String): List<LangPair> {
    val normalizedSource = mapLang(sourceLang)
    val normalizedTarget = mapLang(targetLang)
    val attempts = linkedSetOf(LangPair(normalizedSource, normalizedTarget))

    if (normalizedSource == "zh-CHT") {
      attempts += LangPair("zh-CHS", normalizedTarget)
    }
    if (normalizedTarget == "zh-CHT") {
      attempts += LangPair(normalizedSource, "zh-CHS")
    }
    if (normalizedSource != "auto") {
      attempts += LangPair("auto", normalizedTarget)
    }

    return attempts.toList()
  }

  fun buildUnsupportedMessage(sourceLang: String, targetLang: String): String {
    return "Youdao Web does not currently support translating from $sourceLang to $targetLang."
  }

  fun isErrorCode50(message: String?): Boolean {
    return message?.contains("Youdao webtranslate error code: 50") == true
  }

  private fun isTraditionalChineseVariant(lang: String): Boolean {
    return lang.contains("TW", ignoreCase = true) ||
      lang.contains("HK", ignoreCase = true) ||
      lang.contains("MO", ignoreCase = true) ||
      lang.contains("Hant", ignoreCase = true)
  }
}
