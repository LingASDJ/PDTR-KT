package com.example.pdtranslator

import android.content.Context
import java.util.Locale

object LanguageUtils {

  // Cache ISO 639 language codes for validation
  private val isoLanguages: Set<String> by lazy {
    Locale.getISOLanguages().toSet()
  }

  /**
   * Returns a localized display name for a language code,
   * following the app's current language (Context locale), not system locale.
   *
   * Uses Locale.forLanguageTag() for proper BCP-47 parsing
   * (handles zh-Hans, sr-Latn, pt-BR, etc.)
   *
   * @param langCode  e.g. "en", "zh-CN", "zh-TW", "ja", "ko", "fr", "base"
   * @param context   Android Context — used to get app's current locale from resources
   */
  fun getDisplayName(langCode: String, context: Context): String {
    val appLocale = context.resources.configuration.locales[0]

    // Special code: "base"
    if (langCode == "base") {
      return context.getString(R.string.lang_base)
    }

    // Normalize: underscores → hyphens for BCP-47 compliance (en_US → en-US)
    val normalized = langCode.replace('_', '-')
    // Parse with BCP-47 compliant API (handles script tags, region normalization)
    val locale = Locale.forLanguageTag(normalized)

    // Reject if language is empty or not a real ISO 639 code
    if (locale.language.isEmpty() || !isoLanguages.contains(locale.language)) {
      return langCode.uppercase(Locale.ROOT)
    }

    val displayName = locale.getDisplayName(appLocale)

    // If Locale couldn't resolve, it returns the raw tag — fallback to uppercase
    return if (displayName.isBlank() || displayName == langCode) {
      langCode.uppercase(Locale.ROOT)
    } else {
      displayName.replaceFirstChar { it.titlecase(appLocale) }
    }
  }
}
