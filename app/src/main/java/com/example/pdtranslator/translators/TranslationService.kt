package com.example.pdtranslator.translators

interface TranslationService {
    val name: String
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String
}
