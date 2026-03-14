package com.example.pdtranslator

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.Properties
import kotlin.math.ceil

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
    val displayName: String,
    val content: String,
    val properties: Properties,
    var isModified: Boolean = false
)

class TranslatorViewModel : ViewModel() {

    private val translationService = TranslationService()

    private val languageCodeToNameMap = mapOf(
        "zh" to "中文",
        "en" to "英文",
        "jp" to "日文",
        "ko" to "韩文",
        "fr" to "法文",
        "de" to "德文",
        "ru" to "俄文",
        "es" to "西班牙文",
        "el" to "希腊语"
    )

    fun getLanguageDisplayName(langCode: String): String {
        return languageCodeToNameMap[langCode.lowercase()] ?: langCode.uppercase()
    }

    private val _languageFiles = mutableStateOf<List<LanguageFile>>(emptyList())
    private val _allLanguageFiles = mutableStateOf<Map<String, List<LanguageFile>>>(emptyMap())

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
    
    val languageGroupNames: State<List<String>> = derivedStateOf {
        _allLanguageFiles.value.keys.toList().sorted()
    }

    private val _items = mutableStateOf<List<TranslationItem>>(emptyList())

    private val _translationProgress = mutableStateOf(0f)
    val translationProgress: State<Float> = _translationProgress

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _filterOption = mutableStateOf(FilterOption.ALL)
    val filterOption: State<FilterOption> = _filterOption

    // --- Pagination State ---
    private val _currentPage = mutableStateOf(1)
    val currentPage: State<Int> = _currentPage

    private val itemsPerPage = 20 // Display 20 items per page

    val totalPages: State<Int> = derivedStateOf {
        ceil(filteredItems.value.size.toFloat() / itemsPerPage).toInt()
    }

    val paginatedItems: State<List<TranslationItem>> = derivedStateOf {
        val startIndex = (_currentPage.value - 1) * itemsPerPage
        val endIndex = (startIndex + itemsPerPage).coerceAtMost(filteredItems.value.size)
        if (startIndex < filteredItems.value.size) {
            filteredItems.value.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    private val filteredItems: State<List<TranslationItem>> = derivedStateOf {
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

    val untranslatedItemsCount: State<Int> = derivedStateOf {
        _items.value.count { it.translation.isBlank() }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1 // Reset to first page
    }

    fun onFilterChange(filter: FilterOption) {
        _filterOption.value = filter
        _currentPage.value = 1 // Reset to first page
    }

    // --- Pagination Actions ---
    fun nextPage() {
        if (_currentPage.value < totalPages.value) {
            _currentPage.value++
        }
    }

    fun previousPage() {
        if (_currentPage.value > 1) {
            _currentPage.value--
        }
    }

    fun loadLanguageFiles(contentResolver: ContentResolver, uris: List<Uri>) {
        viewModelScope.launch {
            val fileGroups = mutableMapOf<String, MutableList<LanguageFile>>()
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        val fileName = uri.path?.substringAfterLast('/') ?: "unknown"
                        val groupName = fileName.substringBeforeLast('_')
                        val langCode = extractLangCode(fileName)
                        val displayName = getLanguageDisplayName(langCode)

                        val content = contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        } ?: ""

                        val props = Properties().apply { load(content.reader()) }
                        val langFile = LanguageFile(fileName, langCode, displayName, content, props)

                        fileGroups.getOrPut(groupName) { mutableListOf() }.add(langFile)

                    } catch (e: Exception) {
                        // Handle exceptions, e.g., logging
                    }
                }
            }
            _allLanguageFiles.value = fileGroups
        }
    }

    fun selectLanguageGroup(groupName: String) {
        val groupFiles = _allLanguageFiles.value[groupName] ?: emptyList()
        _languageFiles.value = groupFiles.sortedBy { it.langCode }
        _currentPage.value = 1

        // Set default source and target languages
        _sourceLanguage.value = groupFiles.firstOrNull { it.langCode == "zh" } ?: groupFiles.firstOrNull()
        _targetLanguage.value = groupFiles.firstOrNull { it.langCode != _sourceLanguage.value?.langCode }

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
        _currentPage.value = 1 // Reset to first page
        regenerateItems()
    }

    fun setTargetLanguage(langCode: String) {
        _targetLanguage.value = _languageFiles.value.find { it.langCode == langCode }
        _currentPage.value = 1 // Reset to first page
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
                val originalTranslation = it.translation
                it.copy(translation = newTranslation, isModified = originalTranslation != newTranslation && newTranslation.isNotBlank())
            } else {
                it
            }
        }

        // Update the underlying properties in the LanguageFile
        target.properties.setProperty(key, newTranslation)
        target.isModified = true
        updateProgress()
    }

    fun autoTranslateUntranslatedItems() {
        viewModelScope.launch {
            val source = _sourceLanguage.value ?: return@launch
            val target = _targetLanguage.value ?: return@launch
            val untranslated = _items.value.filter { it.translation.isBlank() }

            untranslated.forEach { item ->
                val translatedText = translationService.translate(item.original, source.langCode, target.langCode)
                if (translatedText != null) {
                    updateTranslation(item.key, translatedText)
                }
            }
        }
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
