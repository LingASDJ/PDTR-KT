package com.example.pdtranslator

import com.example.pdtranslator.translators.GoogleTranslator

interface TranslationService {
    suspend fun translate(text: String, from: String, to: String): String
}

// Factory to get the selected translation engine
fun getTranslationService(engineId: Int): TranslationService {
    // For now, we only have Google Translate
    return GoogleTranslator()
}
