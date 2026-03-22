package com.example.pdtranslator

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class TmEntry(
  val sourceText: String,
  val targetText: String,
  val sourceLang: String,
  val targetLang: String,
  val usageCount: Int = 1,
  val lastUsed: Long
)

data class TmSuggestion(
  val targetText: String,
  val similarity: Float,
  val usageCount: Int
)

class TranslationMemory(private val context: Context) {

  private val lock = Any()
  private val entries = mutableMapOf<String, TmEntry>()
  private val gson = Gson()
  private val file: File get() = File(context.filesDir, "tm.json")

  fun tmKey(sourceText: String, srcLang: String, tgtLang: String): String {
    val encoded = Base64.encodeToString(
      sourceText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
    )
    return "$srcLang|$tgtLang|$encoded"
  }

  fun addEntry(sourceText: String, targetText: String, srcLang: String, tgtLang: String) {
    if (sourceText.isBlank() || targetText.isBlank()) return
    val key = tmKey(sourceText, srcLang, tgtLang)
    synchronized(lock) {
      val existing = entries[key]
      if (existing != null) {
        entries[key] = existing.copy(
          targetText = targetText,
          usageCount = existing.usageCount + 1,
          lastUsed = System.currentTimeMillis()
        )
      } else {
        entries[key] = TmEntry(
          sourceText = sourceText,
          targetText = targetText,
          sourceLang = srcLang,
          targetLang = tgtLang,
          usageCount = 1,
          lastUsed = System.currentTimeMillis()
        )
      }
    }
  }

  fun findMatches(
    sourceText: String,
    srcLang: String,
    tgtLang: String,
    minSimilarity: Float = 0.7f,
    maxResults: Int = 3
  ): List<TmSuggestion> {
    if (sourceText.isBlank()) return emptyList()

    // Exact match first
    val exactKey = tmKey(sourceText, srcLang, tgtLang)
    val snapshot: Map<String, TmEntry>
    synchronized(lock) {
      val exact = entries[exactKey]
      if (exact != null) {
        return listOf(TmSuggestion(exact.targetText, 1.0f, exact.usageCount))
      }
      snapshot = entries.toMap()
    }

    val sourceLen = sourceText.length
    return snapshot.values
      .filter { it.sourceLang == srcLang && it.targetLang == tgtLang }
      .filter { entry ->
        // Length difference filter: skip if lengths differ by more than 50%
        val entryLen = entry.sourceText.length
        val ratio = if (sourceLen > entryLen) entryLen.toFloat() / sourceLen
        else sourceLen.toFloat() / entryLen
        ratio >= 0.5f
      }
      .mapNotNull { entry ->
        val sim = levenshteinSimilarity(sourceText, entry.sourceText)
        if (sim >= minSimilarity) TmSuggestion(entry.targetText, sim, entry.usageCount)
        else null
      }
      .sortedByDescending { it.similarity }
      .take(maxResults)
  }

  fun importFromEntries(
    sourceEntries: Map<String, String>,
    targetEntries: Map<String, String>,
    srcLang: String,
    tgtLang: String
  ) {
    sourceEntries.forEach { (key, sourceText) ->
      val targetText = targetEntries[key]
      if (!targetText.isNullOrBlank() && sourceText.isNotBlank()) {
        addEntry(sourceText, targetText, srcLang, tgtLang)
      }
    }
  }

  suspend fun save() {
    val json: String
    synchronized(lock) {
      json = gson.toJson(entries)
    }
    withContext(Dispatchers.IO) {
      writeTextAtomically(file, json)
    }
  }

  suspend fun load() {
    withContext(Dispatchers.IO) {
      if (file.exists()) {
        try {
          val json = file.readText(Charsets.UTF_8)
          val type = object : TypeToken<Map<String, TmEntry>>() {}.type
          val loaded: Map<String, TmEntry> = gson.fromJson(json, type)
          synchronized(lock) {
            entries.clear()
            entries.putAll(loaded)
          }
        } catch (_: Exception) {
          // Corrupted TM file, ignore
        }
      }
    }
  }

  private fun levenshteinSimilarity(a: String, b: String): Float {
    val maxLen = maxOf(a.length, b.length)
    if (maxLen == 0) return 1.0f
    val distance = levenshteinDistance(a, b)
    return 1.0f - distance.toFloat() / maxLen
  }

  private fun levenshteinDistance(a: String, b: String): Int {
    val m = a.length
    val n = b.length
    val dp = Array(m + 1) { IntArray(n + 1) }
    for (i in 0..m) dp[i][0] = i
    for (j in 0..n) dp[0][j] = j
    for (i in 1..m) {
      for (j in 1..n) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[i][j] = minOf(
          dp[i - 1][j] + 1,
          dp[i][j - 1] + 1,
          dp[i - 1][j - 1] + cost
        )
      }
    }
    return dp[m][n]
  }
}
