package com.example.pdtranslator

import java.util.Locale

object LanguageUtils {

  // Special codes that don't map to standard Locale
  private val specialNames = mapOf(
    "base" to SpecialLang(en = "Base", zh = "基准")
  )

  /**
   * Returns a localized display name for a language code.
   *
   * Uses java.util.Locale to get the native display name of the language,
   * so it works for ANY language code (fr, de, pt-BR, etc.) without
   * maintaining a manual map.
   *
   * @param langCode  e.g. "en", "zh-CN", "zh-TW", "ja", "ko", "fr", "base"
   * @param inLocale  the locale to display the name in (defaults to current system locale)
   */
  fun getDisplayName(langCode: String, inLocale: Locale = Locale.getDefault()): String {
    // Handle special codes
    specialNames[langCode]?.let { special ->
      return if (inLocale.language == "zh") special.zh else special.en
    }

    // Parse langCode into Locale
    val locale = parseLocale(langCode)
    val displayName = locale.getDisplayName(inLocale)

    // If Locale couldn't resolve it, it returns the raw code — capitalize it
    return if (displayName.isBlank() || displayName == langCode) {
      langCode.uppercase(Locale.ROOT)
    } else {
      displayName.replaceFirstChar { it.titlecase(inLocale) }
    }
  }

  /**
   * Parse a language code string into a Locale.
   * Supports formats: "en", "zh-CN", "zh-TW", "pt-BR", etc.
   */
  private fun parseLocale(langCode: String): Locale {
    val parts = langCode.replace("-", "_").split("_")
    return when (parts.size) {
      1 -> Locale(parts[0])
      2 -> Locale(parts[0], parts[1])
      else -> Locale(parts[0], parts[1], parts.drop(2).joinToString("_"))
    }
  }

  private data class SpecialLang(val en: String, val zh: String)
}
