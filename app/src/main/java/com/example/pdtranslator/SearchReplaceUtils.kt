package com.example.pdtranslator

data class SearchableEntry(
  val key: String,
  val sourceValue: String,
  val targetValue: String
)

object SearchReplaceUtils {

  fun matches(
    entry: SearchableEntry,
    query: String,
    caseSensitive: Boolean,
    exactMatch: Boolean
  ): Boolean {
    if (query.isBlank()) return true
    return matchesText(entry.key, query, caseSensitive, exactMatch) ||
      matchesText(entry.sourceValue, query, caseSensitive, exactMatch) ||
      matchesText(entry.targetValue, query, caseSensitive, exactMatch)
  }

  fun matchesText(
    text: String,
    query: String,
    caseSensitive: Boolean,
    exactMatch: Boolean
  ): Boolean {
    if (query.isBlank()) return true
    return if (exactMatch) {
      text.equals(query, ignoreCase = !caseSensitive)
    } else {
      text.contains(query, ignoreCase = !caseSensitive)
    }
  }

  fun replaceTarget(
    original: String,
    search: String,
    replacement: String,
    caseSensitive: Boolean,
    exactMatch: Boolean
  ): String {
    if (search.isBlank()) return original
    return if (exactMatch) {
      if (original.equals(search, ignoreCase = !caseSensitive)) replacement else original
    } else if (caseSensitive) {
      original.replace(search, replacement)
    } else {
      original.replace(Regex(Regex.escape(search), RegexOption.IGNORE_CASE), replacement)
    }
  }
}
