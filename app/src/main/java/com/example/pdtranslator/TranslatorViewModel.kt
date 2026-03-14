package com.example.pdtranslator

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.util.Properties

data class TranslationItem(val key: String, val original: String, val translation: String)

class TranslatorViewModel : ViewModel() {

    private val _translationItems = mutableStateOf<List<TranslationItem>>(emptyList())
    val translationItems: State<List<TranslationItem>> = _translationItems

    private val _translationProgress = mutableStateOf(0f)
    val translationProgress: State<Float> = _translationProgress

    private val _originalContent = mutableStateOf("")
    val originalContent: State<String> = _originalContent

    private val _translatedContent = mutableStateOf("")
    val translatedContent: State<String> = _translatedContent

    fun loadTranslations(originalContent: String, translatedContent: String) {
        _originalContent.value = originalContent
        _translatedContent.value = translatedContent

        if (originalContent.isBlank()) {
            _translationItems.value = emptyList()
            _translationProgress.value = 0f
            return
        }

        val originalProps = Properties().apply { load(originalContent.reader()) }
        val translatedProps = Properties().apply { load(translatedContent.reader()) }

        val items = originalProps.stringPropertyNames().map {
            val originalValue = originalProps.getProperty(it) ?: ""
            val translatedValue = translatedProps.getProperty(it) ?: ""
            TranslationItem(it, originalValue, translatedValue)
        }

        _translationItems.value = items

        val translatedCount = items.count { it.translation.isNotBlank() }
        _translationProgress.value = if (items.isNotEmpty()) {
            translatedCount.toFloat() / items.size
        } else {
            0f
        }
    }
}
