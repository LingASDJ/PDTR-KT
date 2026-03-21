package com.example.pdtranslator

import java.util.Locale

data class LanguageCodeOption(
  val code: String,
  val displayName: String
)

object LanguageCodeCatalog {
  private val regionalCodes = listOf(
    "zh-CN",
    "zh-TW",
    "zh-HK",
    "en-US",
    "en-GB",
    "pt-BR",
    "pt-PT",
    "es-ES",
    "es-MX",
    "fr-CA",
    "ja",
    "ko"
  )

  fun suggestedCodes(additional: Collection<String> = emptyList()): List<String> {
    return (Locale.getISOLanguages().asList() + regionalCodes + additional)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .sorted()
  }

  fun buildOptions(
    codes: Collection<String>,
    displayNameProvider: (String) -> String
  ): List<LanguageCodeOption> {
    return codes
      .distinct()
      .sorted()
      .map { code -> LanguageCodeOption(code = code, displayName = displayNameProvider(code)) }
  }

  fun filter(
    options: List<LanguageCodeOption>,
    query: String
  ): List<LanguageCodeOption> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return options
    return options
      .filter { option ->
        option.code.contains(trimmed, ignoreCase = true) ||
          option.displayName.contains(trimmed, ignoreCase = true)
      }
      .sortedWith(
        compareBy<LanguageCodeOption>(
          { score(it, trimmed) },
          { it.code.length },
          { it.code }
        )
      )
  }

  private fun score(option: LanguageCodeOption, query: String): Int {
    val code = option.code.lowercase(Locale.ROOT)
    val name = option.displayName.lowercase(Locale.ROOT)
    val normalizedQuery = query.lowercase(Locale.ROOT)
    return when {
      code == normalizedQuery -> 0
      code.startsWith(normalizedQuery) -> 1
      name.startsWith(normalizedQuery) -> 2
      code.contains(normalizedQuery) -> 3
      else -> 4
    }
  }
}
