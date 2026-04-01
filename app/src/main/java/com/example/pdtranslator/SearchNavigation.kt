package com.example.pdtranslator

fun findSearchResultScrollIndex(
  entries: List<TranslationEntry>,
  currentSearchResultKey: String?
): Int? {
  if (currentSearchResultKey == null) return null
  val index = entries.indexOfFirst { it.key == currentSearchResultKey }
  return index.takeIf { it >= 0 }
}

fun findGroupedSearchResultScrollIndex(
  entries: List<TranslationEntry>,
  currentSearchResultKey: String?,
  collapsedGroups: Set<String> = emptySet()
): Int? {
  if (currentSearchResultKey == null) return null

  var visibleIndex = 0
  groupEntriesByPrefix(entries).forEach { (prefix, groupedEntries) ->
    visibleIndex += 1
    val entryIndex = groupedEntries.indexOfFirst { it.key == currentSearchResultKey }
    if (entryIndex >= 0) {
      return visibleIndex + entryIndex
    }
    if (prefix !in collapsedGroups) {
      visibleIndex += groupedEntries.size
    }
  }

  return null
}

fun revealCurrentSearchResultGroup(
  collapsedGroups: Set<String>,
  entries: List<TranslationEntry>,
  currentSearchResultKey: String?
): Set<String> {
  if (currentSearchResultKey == null) return collapsedGroups
  val targetPrefix = entries.firstOrNull { it.key == currentSearchResultKey }
    ?.let { entryPrefix(it.key) }
    ?: return collapsedGroups
  return collapsedGroups - targetPrefix
}

fun groupEntriesByPrefix(entries: List<TranslationEntry>): List<Pair<String, List<TranslationEntry>>> {
  val groupedEntries = LinkedHashMap<String, MutableList<TranslationEntry>>()
  entries.forEach { entry ->
    groupedEntries.getOrPut(entryPrefix(entry.key)) { mutableListOf() }.add(entry)
  }
  return groupedEntries.map { (prefix, grouped) -> prefix to grouped.toList() }
}

fun entryPrefix(key: String): String {
  val dotIndex = key.indexOf('.')
  return if (dotIndex > 0) key.substring(0, dotIndex) else key
}
