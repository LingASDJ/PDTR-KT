package com.example.pdtranslator

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.util.Properties

data class TranslationItem(
    val key: String, 
    val original: String, 
    val translation: String,
    val isModified: Boolean = false
)

enum class FilterOption {
    ALL, UNTRANSLATED, MODIFIED
}

class TranslatorViewModel : ViewModel() {

    private val _originalItems = mutableStateOf<List<TranslationItem>>(emptyList())
    private val _translationProgress = mutableStateOf(0f)
    val translationProgress: State<Float> = _translationProgress

    private val _originalContent = mutableStateOf("")
    val originalContent: State<String> = _originalContent

    private val _translatedContent = mutableStateOf("")
    val translatedContent: State<String> = _translatedContent

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _filterOption = mutableState of(FilterOption.ALL)
    val filterOption: State<FilterOption> = _filterOption

    val filteredItems: State<List<TranslationItem>> = derivedStateOf {
        val query = _searchQuery.value.lowercase()
        val items = _originalItems.value
        
        val searchedItems = if (query.isBlank()) {
            items
        } else {
            items.filter { it.key.lowercase().contains(query) }
        }

        when (_filterOption.value) {
            FilterOption.ALL -> searchedItems
            FilterOption.UNTRANSLATED -> searchedItems.filter { it.translation.isBlank() }
            FilterOption.MODIFIED -> searchedItems.filter { it.isModified }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: FilterOption) {
        _filterOption.value = filter
    }

    fun loadTranslations(originalContent: String, translatedContent: String) {
        _originalContent.value = originalContent
        _translatedContent.value = translatedContent

        if (originalContent.isBlank()) {
            _originalItems.value = emptyList()
            updateProgress()
            return
        }

        val originalProps = Properties().apply { load(originalContent.reader()) }
        val translatedProps = Properties().apply { load(translatedContent.reader()) }

        val items = originalProps.stringPropertyNames().map {
            val originalValue = originalProps.getProperty(it) ?: ""
            val translatedValue = translatedProps.getProperty(it) ?: ""
            TranslationItem(it, originalValue, translatedValue)
        }

        _originalItems.value = items
        updateProgress()
    }

    fun updateTranslation(key: String, newTranslation: String) {
        _originalItems.value = _originalItems.value.map {
            if (it.key == key) {
                it.copy(translation = newTranslation, isModified = true)
            } else {
                it
            }
        }
        updateProgress()
    }

    private fun updateProgress() {
        val items = _originalItems.value
        val translatedCount = items.count { it.translation.isNotBlank() }
        _translationProgress.value = if (items.isNotEmpty()) {
            translatedCount.toFloat() / items.size
        } else {
            0f
        }
    }
}
