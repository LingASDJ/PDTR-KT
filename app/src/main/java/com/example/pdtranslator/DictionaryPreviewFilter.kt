package com.example.pdtranslator

import java.util.Locale

data class DictionaryPreviewItem(
  val rawKey: String,
  val propKey: String,
  val groupName: String?,
  val langPair: String?,
  val sourceText: String?,
  val translation: String,
  val timestamp: Long
)

object DictionaryPreviewFilter {
  fun filter(dictionary: NamedDictionary, query: String): List<DictionaryPreviewItem> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    return dictionary.entries.entries
      .map { (key, entry) ->
        val parts = key.split('|')
        val (propKey, groupName, langPair) = when (parts.size) {
          4 -> Triple(parts[3], parts[0], "${parts[1]} → ${parts[2]}")
          3 -> Triple(parts[2], null, "${parts[0]} → ${parts[1]}")
          else -> Triple(key, null, null)
        }
        DictionaryPreviewItem(
          rawKey = key,
          propKey = propKey,
          groupName = groupName,
          langPair = langPair,
          sourceText = entry.sourceText,
          translation = entry.translation,
          timestamp = entry.timestamp
        )
      }
      .sortedBy { it.propKey }
      .filter { item ->
        normalizedQuery.isBlank() ||
          item.propKey.lowercase(Locale.ROOT).contains(normalizedQuery) ||
          item.groupName.orEmpty().lowercase(Locale.ROOT).contains(normalizedQuery) ||
          item.sourceText.orEmpty().lowercase(Locale.ROOT).contains(normalizedQuery) ||
          item.translation.lowercase(Locale.ROOT).contains(normalizedQuery)
      }
  }
}
