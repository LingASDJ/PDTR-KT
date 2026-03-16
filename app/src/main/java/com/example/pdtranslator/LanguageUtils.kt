package com.example.pdtranslator

import java.util.Locale

object LanguageUtils {
    // Map of language codes to their display names.
    private val languageNameMap = mapOf(
        "en" to "English",
        "zh" to "简体中文",
        "chk" to "繁體中文",
        "ru" to "Русский",
        "ko" to "한국어",
        "ja" to "日本語",
        "base" to "基础" // 'base' is used for base language files
    )

    /**
     * Returns the display name for a given language code.
     * If the code is not found, it returns the code itself in uppercase.
     */
    fun getDisplayName(langCode: String): String {
        return languageNameMap[langCode] ?: langCode.uppercase(Locale.ROOT)
    }
}
