
package com.example.pdtranslator

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdtranslator.translators.GoogleTranslator
import com.example.pdtranslator.translators.TranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.StringReader
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// --- Data Classes & Enums ---

data class TranslationEntry(
    val key: String,
    val sourceValue: String,
    var targetValue: String,
    val originalTargetValue: String, // Hold the initial value to check for real modification
    val isUntranslated: Boolean,
    var isModified: Boolean = false, // Now represents a staged change
    val isMissing: Boolean = false,
    val isIdentical: Boolean = false
)

data class LanguageData(val fileName: String, val properties: Properties)

data class LanguageGroup(
    val name: String,
    val languages: Map<String, LanguageData> // Key: langCode (e.g., "en", "base")
)

enum class FilterState { ALL, UNTRANSLATED, TRANSLATED, MODIFIED, MISSING }

enum class ThemeColor {
    DEFAULT, M3, GREEN, LAVENDER
}

// --- ViewModel ---

class TranslatorViewModel : ViewModel() {

    // --- Internal State ---
    private val _languageGroups = MutableStateFlow<List<LanguageGroup>>(emptyList())
    private val _allEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())
    private val _modifiedEntries = MutableStateFlow<Map<String, Properties>>(emptyMap())
    private val _showAboutDialog = MutableStateFlow(false)
    private val _translationEngine = MutableStateFlow<TranslationService>(GoogleTranslator())
    private val _themeColor = MutableStateFlow(ThemeColor.DEFAULT)

    // --- UI State Exposed as StateFlows ---
    val languageGroupNames = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val displayEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())

    val selectedGroupName = MutableStateFlow<String?>(null)
    val sourceLangCode = MutableStateFlow<String?>(null)
    val targetLangCode = MutableStateFlow<String?>(null)

    // Search and Replace State
    val searchQuery = MutableStateFlow("")
    val replaceQuery = MutableStateFlow("")
    val isCaseSensitive = MutableStateFlow(false)
    val isExactMatch = MutableStateFlow(false)

    val filterState = MutableStateFlow(FilterState.ALL)

    // Pagination
    val currentPage = MutableStateFlow(1)
    val pageSize = 20
    val totalPages = MutableStateFlow(1)

    // Smart Info Bar State
    val infoBarText = MutableStateFlow("")

    val isSaveEnabled = MutableStateFlow(false)
    val showAboutDialog = _showAboutDialog.asStateFlow()
    val themeColor = _themeColor.asStateFlow()
    val translationEngine = _translationEngine.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                _allEntries, searchQuery, isCaseSensitive, isExactMatch, filterState, currentPage
            ) { flows ->
                val entries = flows[0] as List<TranslationEntry>
                val search = flows[1] as String
                val caseSensitive = flows[2] as Boolean
                val exactMatch = flows[3] as Boolean
                val filter = flows[4] as FilterState
                val page = flows[5] as Int

                // --- Filtering Logic ---
                val filtered = entries.filter { entry ->
                    val matchesFilter = when (filter) {
                        FilterState.ALL -> true
                        FilterState.UNTRANSLATED -> entry.isUntranslated
                        FilterState.TRANSLATED -> !entry.isUntranslated && !entry.isMissing
                        FilterState.MODIFIED -> entry.isModified
                        FilterState.MISSING -> entry.isMissing
                    }

                    val matchesSearch = if (search.isBlank()) {
                        true
                    } else {
                        val sourceMatch = if (exactMatch) {
                            entry.sourceValue.equals(search, ignoreCase = !caseSensitive)
                        } else {
                            entry.sourceValue.contains(search, ignoreCase = !caseSensitive)
                        }
                        val keyMatch = entry.key.contains(search, ignoreCase = !caseSensitive)
                        sourceMatch || keyMatch
                    }
                    matchesFilter && matchesSearch
                }

                // --- Pagination Logic ---
                totalPages.value = (filtered.size + pageSize - 1) / pageSize.coerceAtLeast(1)
                val newPage = if (page > totalPages.value) 1 else page
                if (page != newPage) currentPage.value = newPage

                displayEntries.value = filtered.chunked(pageSize).getOrElse(newPage - 1) { emptyList() }

                // --- Smart Info Bar Logic ---
                if (filter == FilterState.UNTRANSLATED) {
                    val total = entries.size
                    val translated = total - entries.count { it.isUntranslated }
                    val progress = if (total == 0) 0f else translated.toFloat() / total
                    infoBarText.value = "翻译进度: ${(progress * 100).toInt()}%%"
                } else {
                    infoBarText.value = "语言组总条目: ${entries.size}"
                }
            }.collect {}
        }

        viewModelScope.launch {
            _modifiedEntries.collect {
                isSaveEnabled.value = it.isNotEmpty() && it.any { entry -> entry.value.isNotEmpty() }
            }
        }
    }

    // --- Public Intent Functions ---

    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setReplaceQuery(query: String) { replaceQuery.value = query }
    fun setCaseSensitive(isSensitive: Boolean) { isCaseSensitive.value = isSensitive }
    fun setExactMatch(isExact: Boolean) { isExactMatch.value = isExact }
    fun setFilter(filter: FilterState) { filterState.value = filter }
    fun nextPage() { if (currentPage.value < totalPages.value) currentPage.value++ }
    fun previousPage() { if (currentPage.value > 1) currentPage.value-- }
    fun setShowAboutDialog(show: Boolean) { _showAboutDialog.value = show }
    fun setThemeColor(theme: ThemeColor) { _themeColor.value = theme }
    fun setTranslationEngine(engine: TranslationService) { _translationEngine.value = engine }

    fun loadFilesFromUris(resolver: ContentResolver, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = mutableMapOf<String, MutableMap<String, LanguageData>>()
            for (uri in uris) {
                val fileName = getFileName(resolver, uri) ?: continue
                val (baseName, langCode) = parseFileName(fileName)
                if (langCode != null) {
                    val content = resolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).readText()
                    } ?: continue

                    val filteredContent = content.lines().filter {
                        !it.trim().startsWith("#") && !it.trim().startsWith("//")
                    }.joinToString("\n")

                    val props = Properties().apply { load(StringReader(filteredContent)) }
                    groups.getOrPut(baseName) { mutableMapOf() }[langCode] = LanguageData(fileName, props)
                }
            }
            processLoadedGroups(groups)
        }
    }

    fun selectGroup(name: String) {
        selectedGroupName.value = name
        sourceLangCode.value = null
        targetLangCode.value = null
        _allEntries.value = emptyList()
        availableLanguages.value = _languageGroups.value.find { it.name == name }
            ?.languages?.keys?.sorted() ?: emptyList()
    }

    fun selectSourceLanguage(code: String) {
        sourceLangCode.value = code
        if (targetLangCode.value != null) regenerateEntries()
    }

    fun selectTargetLanguage(code: String) {
        targetLangCode.value = code
        if (sourceLangCode.value != null) regenerateEntries()
    }

    fun stageChange(key: String, newTargetValue: String) {
        val langCode = targetLangCode.value ?: return

        _allEntries.update { currentEntries ->
            currentEntries.map { entry ->
                if (entry.key == key) {
                    val isTrulyModified = newTargetValue != entry.originalTargetValue
                    if (isTrulyModified) {
                        updateModifiedProperties(langCode, key, newTargetValue)
                    } else {
                        removeModifiedProperty(langCode, key)
                    }
                    entry.copy(
                        targetValue = newTargetValue,
                        isModified = isTrulyModified,
                        isUntranslated = newTargetValue.isBlank() || (newTargetValue == entry.sourceValue)
                    )
                } else entry
            }
        }
    }

    fun autoTranslateEntry(entry: TranslationEntry) {
        viewModelScope.launch {
            val source = sourceLangCode.value ?: return@launch
            val target = targetLangCode.value ?: return@launch
            val translatedText = _translationEngine.value.translate(entry.sourceValue, source, target)
            stageChange(entry.key, translatedText)
        }
    }
    
    fun replaceAll() {
        val search = searchQuery.value
        val replace = replaceQuery.value
        if (search.isBlank()) return

        val caseSensitive = isCaseSensitive.value
        val exactMatch = isExactMatch.value
        val langCode = targetLangCode.value ?: return

        _allEntries.update { currentEntries ->
            currentEntries.map { entry ->
                 val matches = if (exactMatch) {
                    entry.sourceValue.equals(search, ignoreCase = !caseSensitive)
                } else {
                    entry.sourceValue.contains(search, ignoreCase = !caseSensitive)
                }
                
                if (matches) {
                    val newTargetValue = entry.sourceValue.replace(search, replace, ignoreCase = !caseSensitive)
                    val isTrulyModified = newTargetValue != entry.originalTargetValue
                     if (isTrulyModified) {
                        updateModifiedProperties(langCode, entry.key, newTargetValue)
                    } else {
                        removeModifiedProperty(langCode, entry.key)
                    }
                    
                    entry.copy(
                        targetValue = newTargetValue,
                        isModified = isTrulyModified,
                        isUntranslated = newTargetValue.isBlank() || (newTargetValue == entry.sourceValue)
                    )
                } else {
                    entry
                }
            }
        }
    }


    fun saveChangesToZip(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val groupName = selectedGroupName.value ?: return@launch
            val group = _languageGroups.value.find { it.name == groupName } ?: return@launch

            resolver.openOutputStream(uri)?.use {
                ZipOutputStream(it).use { zos ->
                    group.languages.forEach { (langCode, langData) ->
                        val finalProps = Properties()
                        val originalContent = langData.properties.stringPropertyNames().associateWith { langData.properties.getProperty(it) }
                        finalProps.putAll(originalContent)
                        
                        _modifiedEntries.value[langCode]?.let { mods -> finalProps.putAll(mods) }

                        val entryPath = "${group.name}/${langData.fileName}"
                        zos.putNextEntry(ZipEntry(entryPath))
                        val writer = OutputStreamWriter(zos)
                        finalProps.store(writer, "PDTranslator Modified File")
                        writer.flush()
                        zos.closeEntry()
                    }
                }
            }
            _modifiedEntries.update { currentMods ->
                val newMods = currentMods.toMutableMap()
                group.languages.keys.forEach { langCode -> newMods.remove(langCode) }
                newMods
            }
            regenerateEntries()
        }
    }

    private fun updateModifiedProperties(langCode: String, key: String, value: String) {
        _modifiedEntries.update { currentMods ->
            val newMods = currentMods.toMutableMap()
            val langProps = newMods.getOrPut(langCode) { Properties() }
            langProps.setProperty(key, value)
            newMods
        }
    }

    private fun removeModifiedProperty(langCode: String, key: String) {
        _modifiedEntries.update { currentMods ->
            val newMods = currentMods.toMutableMap()
            newMods[langCode]?.let {
                it.remove(key)
                if (it.isEmpty) {
                    newMods.remove(langCode)
                }
            }
            newMods
        }
    }
    
    private fun processLoadedGroups(groups: Map<String, Map<String, LanguageData>>) {
        _languageGroups.value = groups.map { (name, languages) -> LanguageGroup(name, languages) }
        languageGroupNames.value = _languageGroups.value.map { it.name }.sorted()
        resetAllSelections()
    }

    private fun resetAllSelections() {
        selectedGroupName.value = null
        sourceLangCode.value = null
        targetLangCode.value = null
        availableLanguages.value = emptyList()
        _allEntries.value = emptyList()
        _modifiedEntries.value = emptyMap()
    }

    private fun regenerateEntries() {
        val sourceCode = sourceLangCode.value
        val targetCode = targetLangCode.value
        val group = _languageGroups.value.find { it.name == selectedGroupName.value }

        if (sourceCode == null || targetCode == null || group == null) {
            _allEntries.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val sourceProps = group.languages[sourceCode]?.properties ?: Properties()
            val targetProps = group.languages[targetCode]?.properties ?: Properties()
            val modifiedTargetProps = _modifiedEntries.value[targetCode]

            val sortedKeys = sourceProps.stringPropertyNames().sorted()

            val newEntries = sortedKeys.map { key ->
                val sourceValue = sourceProps.getProperty(key, "")
                val isMissing = !targetProps.containsKey(key)
                val originalTargetValue = if (isMissing) "" else targetProps.getProperty(key, "")
                val isModified = modifiedTargetProps?.containsKey(key) ?: false
                val finalTargetValue = if (isModified) modifiedTargetProps!!.getProperty(key) else originalTargetValue
                val isIdentical = sourceValue == finalTargetValue && finalTargetValue.isNotBlank()
                val isUntranslated = !isMissing && (finalTargetValue.isBlank() || isIdentical)

                TranslationEntry(
                    key = key,
                    sourceValue = sourceValue,
                    targetValue = finalTargetValue,
                    originalTargetValue = originalTargetValue,
                    isUntranslated = isUntranslated,
                    isModified = isModified,
                    isMissing = isMissing,
                    isIdentical = isIdentical
                )
            }
            _allEntries.value = newEntries
        }
    }
    
    private fun parseFileName(fileName: String): Pair<String, String?> {
        val base = fileName.substringBeforeLast('.')
        val parts = base.split('_')
        return if (parts.size > 1) {
            val langCode = parts.last()
            val baseName = parts.dropLast(1).joinToString("_")
            Pair(baseName, langCode)
        } else {
            Pair(base, "base")
        }
    }

    private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
        return resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) cursor.getString(nameIndex) else null
            } else null
        }
    }
}
