package com.example.pdtranslator

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.io.StringWriter
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

data class LanguageFile(
    val fileName: String,
    val langCode: String,
    val content: String,
    val properties: Properties,
    var isModified: Boolean = false
)

class TranslatorViewModel : ViewModel() {

    private val _languageFiles = mutableStateOf<List<LanguageFile>>(emptyList())

    private val _sourceLanguage = mutableStateOf<LanguageFile?>(null)
    val sourceLanguage: State<LanguageFile?> = _sourceLanguage

    private val _targetLanguage = mutableStateOf<LanguageFile?>(null)
    val targetLanguage: State<LanguageFile?> = _targetLanguage

    val availableSourceLanguages: State<List<LanguageFile>> = derivedStateOf {
        _languageFiles.value
    }
    val availableTargetLanguages: State<List<LanguageFile>> = derivedStateOf {
        _languageFiles.value.filter { it.langCode != _sourceLanguage.value?.langCode }
    }

    private val _items = mutableStateOf<List<TranslationItem>>(emptyList())

    private val _translationProgress = mutableStateOf(0f)
    val translationProgress: State<Float> = _translationProgress

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _filterOption = mutableStateOf(FilterOption.ALL)
    val filterOption: State<FilterOption> = _filterOption

    val filteredItems: State<List<TranslationItem>> = derivedStateOf {
        val query = _searchQuery.value.lowercase()
        val items = _items.value

        val searchedItems = if (query.isBlank()) {
            items
        } else {
            items.filter { it.key.lowercase().contains(query) || it.original.lowercase().contains(query) }
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

    fun loadLanguageGroup(fileContents: Map<String, String>) {
        val langFiles = fileContents.mapNotNull { (path, content) ->
            val fileName = path.substringAfterLast('/')
            val langCode = extractLangCode(fileName)
            val props = Properties().apply { load(content.reader()) }
            LanguageFile(fileName, langCode, content, props)
        }
        _languageFiles.value = langFiles.sortedBy { it.langCode }

        // Set default source and target languages
        _sourceLanguage.value = langFiles.firstOrNull { it.langCode == "zh" } ?: langFiles.firstOrNull()
        _targetLanguage.value = langFiles.firstOrNull { it.langCode != _sourceLanguage.value?.langCode }

        regenerateItems()
    }

    private fun extractLangCode(fileName: String): String {
        // e.g., actor_en.properties -> en
        val baseName = fileName.substringBeforeLast('.')
        val langPart = baseName.substringAfterLast('_')
        return if (langPart.isNotBlank() && langPart != baseName) langPart else "zh" // Default to zh if no code
    }

    fun setSourceLanguage(langCode: String) {
        _sourceLanguage.value = _languageFiles.value.find { it.langCode == langCode }
        // If the target is same as new source, find a new target
        if (_targetLanguage.value?.langCode == langCode) {
            _targetLanguage.value = _languageFiles.value.firstOrNull { it.langCode != langCode }
        }
        regenerateItems()
    }

    fun setTargetLanguage(langCode: String) {
        _targetLanguage.value = _languageFiles.value.find { it.langCode == langCode }
        regenerateItems()
    }

    private fun regenerateItems() {
        val source = _sourceLanguage.value
        val target = _targetLanguage.value

        if (source == null || target == null) {
            _items.value = emptyList()
            updateProgress()
            return
        }

        val sourceKeys = source.properties.stringPropertyNames()
        val items = sourceKeys.map { key ->
            val originalValue = source.properties.getProperty(key) ?: ""
            val translatedValue = target.properties.getProperty(key) ?: ""
            // Check if this key was modified in a previous session (not implemented yet, but for future)
            TranslationItem(key, originalValue, translatedValue, isModified = false)
        }
        _items.value = items.sortedBy { it.key }
        updateProgress()
    }

    fun updateTranslation(key: String, newTranslation: String) {
        val target = _targetLanguage.value ?: return

        // Update the live item for immediate UI feedback
        _items.value = _items.value.map {
            if (it.key == key) {
                // Only mark as modified if the text actually changed
                it.copy(translation = newTranslation, isModified = it.translation != newTranslation)
            } else {
                it
            }
        }

        // Update the underlying properties in the LanguageFile
        target.properties.setProperty(key, newTranslation)
        target.isModified = true
        updateProgress()
    }


    fun getModifiedContentForTarget(): String? {
        val target = _targetLanguage.value
        if (target == null || !target.isModified) {
            return null
        }

        val writer = StringWriter()
        // Use a custom writer to save properties in a sorted order
        val sortedProps = object : Properties() {
            @Synchronized
            override fun keys(): java.util.Enumeration<Any> {
                return java.util.Collections.enumeration(super.keys.map { it as String }.sorted())
            }
        }
        sortedProps.putAll(target.properties)
        sortedProps.store(writer, null)
        return writer.toString()
    }


    private fun updateProgress() {
        val items = _items.value
        if (items.isEmpty()) {
            _translationProgress.value = 0f
            return
        }
        val translatedCount = items.count { it.translation.isNotBlank() }
        _translationProgress.value = translatedCount.toFloat() / items.size
    }
}
