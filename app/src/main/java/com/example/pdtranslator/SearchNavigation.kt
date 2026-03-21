package com.example.pdtranslator

fun findSearchResultScrollIndex(
  entries: List<TranslationEntry>,
  currentSearchResultKey: String?
): Int? {
  if (currentSearchResultKey == null) return null
  val index = entries.indexOfFirst { it.key == currentSearchResultKey }
  return index.takeIf { it >= 0 }
}
