package com.example.pdtranslator

data class ProgressEntry(
  val sourceValue: String,
  val targetValue: String,
  val isDeleted: Boolean = false
)

data class TranslationProgressState(
  val ratio: Float,
  val translatedCount: Int,
  val totalCount: Int
)

object TranslationProgressCalculator {
  fun calculate(entries: List<ProgressEntry>): TranslationProgressState {
    val activeEntries = entries.filterNot { it.isDeleted }
    val total = activeEntries.size
    val translated = activeEntries.count { entry ->
      entry.targetValue.isNotBlank() && entry.targetValue != entry.sourceValue
    }
    val ratio = if (total == 0) 0f else translated.toFloat() / total
    return TranslationProgressState(
      ratio = ratio,
      translatedCount = translated,
      totalCount = total
    )
  }
}
