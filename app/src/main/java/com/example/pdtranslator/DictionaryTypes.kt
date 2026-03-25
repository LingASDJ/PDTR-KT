package com.example.pdtranslator

data class DictEntry(
  val sourceText: String? = null,
  val translation: String,
  val timestamp: Long,
  val reviewed: Boolean = false
)
