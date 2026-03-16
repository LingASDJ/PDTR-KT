package com.example.pdtranslator

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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

// --- UI Events ---
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

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
    private val _stagedChanges = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _showAboutDialog = MutableStateFlow(false)
    private val _themeColor = MutableStateFlow(ThemeColor.DEFAULT)
    private val _isSearchCardVisible = MutableStateFlow(true)
    private val _missingEntriesCount = MutableStateFlow(0)

    // --- UI Events Channel ---
    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    // --- UI State Exposed as StateFlows ---
    val languageGroupNames = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val displayEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())
    val stagedChanges = _stagedChanges.asStateFlow()
    val isSearchCardVisible = _isSearchCardVisible.asStateFlow()
    val missingEntriesCount = _missingEntriesCount.asStateFlow()

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

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                _allEntries, searchQuery, isCaseSensitive, isExactMatch, filterState, currentPage, _stagedChanges
            ) { flows ->
                val entries = flows[0] as List<TranslationEntry>
                val search = flows[1] as String
                val caseSensitive = flows[2] as Boolean
                val exactMatch = flows[3] as Boolean
                val filter = flows[4] as FilterState
                val page = flows[5] as Int
                val staged = flows[6] as Map<String, String>

                // --- Filtering and Mapping Logic ---
                val processedEntries = entries.map { entry ->
                    staged[entry.key]?.let { stagedValue ->
                        val modified = stagedValue != entry.originalTargetValue
                        entry.copy(targetValue = stagedValue, isModified = modified)
                    } ?: entry.copy(isModified = false, targetValue = entry.originalTargetValue)
                }

                val filtered = processedEntries.filter { entry ->
                    val matchesFilter = when (filter) {
                        FilterState.ALL -> true
                        FilterState.UNTRANSLATED -> entry.isUntranslated || (entry.isMissing && staged.containsKey(entry.key))
                        FilterState.TRANSLATED -> !entry.isUntranslated && !entry.isMissing
                        FilterState.MODIFIED -> entry.isModified
                        FilterState.MISSING -> entry.isMissing && !staged.containsKey(entry.key)
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
                val newPage = page.coerceIn(1, totalPages.value.coerceAtLeast(1))
                if (page != newPage) currentPage.value = newPage

                displayEntries.value = filtered.chunked(pageSize).getOrElse(newPage - 1) { emptyList() }

                // --- Smart Info Bar Logic ---
                if (filter == FilterState.UNTRANSLATED) {
                    val total = entries.size
                    val translated = total - entries.count { it.isUntranslated }
                    val progress = if (total == 0) 0f else translated.toFloat() / total
                    infoBarText.value = "翻译进度: ${(progress * 100).toInt()}%"
                } else {
                    infoBarText.value = "语言组总条目: ${entries.size}"
                }
            }.collect {}
        }

        viewModelScope.launch {
            _stagedChanges.collect {
                isSaveEnabled.value = it.isNotEmpty()
            }
        }
    }

    // --- Public Intent Functions ---

    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setReplaceQuery(query: String) { replaceQuery.value = query }
    fun setCaseSensitive(isSensitive: Boolean) { isCaseSensitive.value = isSensitive }
    fun setExactMatch(isExact: Boolean) { isExactMatch.value = isExact }
    fun setFilter(filter: FilterState) {
        filterState.value = filter
        currentPage.value = 1
        // Regenerate entries when the filter changes to ensure the list is correct.
        regenerateEntries()
    }
    fun nextPage() { if (currentPage.value < totalPages.value) currentPage.value++ }
    fun previousPage() { if (currentPage.value > 1) currentPage.value-- }
    fun setShowAboutDialog(show: Boolean) { _showAboutDialog.value = show }
    fun setThemeColor(theme: ThemeColor) { _themeColor.value = theme }
    fun toggleSearchCardVisibility() { _isSearchCardVisible.value = !_isSearchCardVisible.value }

    fun loadFilesFromUris(resolver: ContentResolver, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = mutableMapOf<String, MutableMap<String, LanguageData>>()
            uris.forEach { uri ->
                val fileName = getFileName(resolver, uri) ?: return@forEach
                if (fileName.endsWith(".zip")) {
                    loadFromZip(resolver, uri, groups)
                } else {
                    loadFile(resolver, uri, fileName, groups)
                }
            }
            processLoadedGroups(groups)
        }
    }

    private fun loadFromZip(resolver: ContentResolver, uri: Uri, groups: MutableMap<String, MutableMap<String, LanguageData>>) {
        resolver.openInputStream(uri)?.use { zipInputStream ->
            ZipInputStream(zipInputStream).use { zis ->
                var zipEntry: ZipEntry?
                while (zis.nextEntry.also { zipEntry = it } != null) {
                    zipEntry?.name?.let { entryName ->
                         if (!entryName.endsWith("/")) {
                            val (baseName, langCode) = parseFileName(entryName.substringAfterLast('/'))
                            if (langCode != null) {
                                val content = BufferedReader(InputStreamReader(zis)).readText()
                                val props = Properties().apply { load(StringReader(content)) }
                                groups.getOrPut(baseName) { mutableMapOf() }[langCode] = LanguageData(entryName, props)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadFile(resolver: ContentResolver, uri: Uri, fileName: String, groups: MutableMap<String, MutableMap<String, LanguageData>>) {
        val (baseName, langCode) = parseFileName(fileName)
        if (langCode != null) {
            resolver.openInputStream(uri)?.use { stream ->
                val content = BufferedReader(InputStreamReader(stream)).readText()
                val preprocessedContent = content.replace(Regex("\\u(?![0-9a-fA-F]{4})"), "\\u")
                try {
                    val props = Properties().apply { load(StringReader(preprocessedContent)) }
                    groups.getOrPut(baseName) { mutableMapOf() }[langCode] = LanguageData(fileName, props)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    fun selectGroup(name: String) {
        selectedGroupName.value = name
        sourceLangCode.value = null
        targetLangCode.value = null
        _allEntries.value = emptyList()
        _stagedChanges.value = emptyMap()
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
        _stagedChanges.update { currentStaged ->
            val newStaged = currentStaged.toMutableMap()
            newStaged[key] = newTargetValue
            newStaged
        }
    }

    fun unstageChange(key: String) {
        _stagedChanges.update { currentStaged ->
            val newStaged = currentStaged.toMutableMap()
            newStaged.remove(key)
            newStaged
        }
    }

    fun saveChangesToZip(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val groupName = selectedGroupName.value ?: return@launch
            val group = _languageGroups.value.find { it.name == groupName } ?: return@launch
            val targetCode = targetLangCode.value ?: return@launch

            val staged = _stagedChanges.value

            resolver.openOutputStream(uri)?.use {
                ZipOutputStream(it).use { zos ->
                    group.languages.forEach { (langCode, langData) ->
                        val finalProps = Properties()
                        finalProps.putAll(langData.properties)

                        if (langCode == targetCode) {
                            staged.forEach { (key, value) ->
                                finalProps.setProperty(key, value)
                            }
                        }
                        
                        val entryPath = langData.fileName
                        zos.putNextEntry(ZipEntry(entryPath))
                        val writer = OutputStreamWriter(zos, "UTF-8")
                        finalProps.store(writer, "PDTranslator Modified File")
                        writer.flush()
                        zos.closeEntry()
                    }
                }
            }
            _stagedChanges.value = emptyMap()
            regenerateEntries()
        }
    }

    private fun processLoadedGroups(groups: Map<String, Map<String, LanguageData>>) {
        _languageGroups.value = groups.map { (name, languages) -> LanguageGroup(name, languages) }.sortedBy { it.name }
        languageGroupNames.value = _languageGroups.value.map { it.name }
        resetAllSelections()
    }

    private fun resetAllSelections() {
        selectedGroupName.value = null
        sourceLangCode.value = null
        targetLangCode.value = null
        availableLanguages.value = emptyList()
        _allEntries.value = emptyList()
        _stagedChanges.value = emptyMap()
    }

    private fun regenerateEntries() {
        val sourceCode = sourceLangCode.value
        val targetCode = targetLangCode.value
        val group = _languageGroups.value.find { it.name == selectedGroupName.value }

        if (sourceCode == null || targetCode == null || group == null) {
            _allEntries.value = emptyList()
            _missingEntriesCount.value = 0
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val sourceProps = group.languages[sourceCode]?.properties ?: Properties()
            val targetProps = group.languages[targetCode]?.properties ?: Properties()

            val allKeys = (sourceProps.keys + targetProps.keys).mapNotNull { it as? String }.distinct()
            val stagedKeys = _stagedChanges.value.keys
            val sortedKeys = (allKeys + stagedKeys).distinct().sorted()


            var missingCount = 0
            val newEntries = sortedKeys.map { key ->
                val sourceValue = sourceProps.getProperty(key, "")
                val isMissingInFile = !targetProps.containsKey(key)
                val isStaged = _stagedChanges.value.containsKey(key)

                val originalTargetValue = if (isMissingInFile) "" else targetProps.getProperty(key, "")
                val finalTargetValue = _stagedChanges.value[key] ?: originalTargetValue
                
                val isModified = isStaged && (finalTargetValue != originalTargetValue)

                if (isMissingInFile && !isStaged) missingCount++

                val isIdentical = sourceValue == finalTargetValue && finalTargetValue.isNotBlank()
                val isUntranslated = finalTargetValue.isBlank() || isIdentical

                TranslationEntry(
                    key = key,
                    sourceValue = sourceValue,
                    targetValue = finalTargetValue,
                    originalTargetValue = originalTargetValue,
                    isUntranslated = isUntranslated,
                    isModified = isModified,
                    isMissing = isMissingInFile && !isStaged,
                    isIdentical = isIdentical
                )
            }
            _allEntries.value = newEntries
            _missingEntriesCount.value = missingCount
        }
    }

    fun fillMissingEntries() {
        viewModelScope.launch(Dispatchers.Default) {
            val sourceCode = sourceLangCode.value
            val targetCode = targetLangCode.value
            val group = _languageGroups.value.find { it.name == selectedGroupName.value }

            if (sourceCode == null || targetCode == null || group == null) return@launch

            val sourceProps = group.languages[sourceCode]?.properties ?: Properties()
            val targetProps = group.languages[targetCode]?.properties ?: Properties()

            val missingKeys = sourceProps.keys.mapNotNull { it as? String }
                .filter { !targetProps.containsKey(it) }

            if (missingKeys.isEmpty()) {
                _uiEvents.send(UiEvent.ShowSnackbar("没有发现缺失的字段。"))
                return@launch
            }

            val currentStaged = _stagedChanges.value
            val newStagedChanges = currentStaged.toMutableMap()
            var addedCount = 0

            missingKeys.forEach { key ->
                if (!currentStaged.containsKey(key)) {
                    newStagedChanges[key] = "" // Add empty string for the missing key
                    addedCount++
                }
            }

            if (addedCount > 0) {
                _stagedChanges.value = newStagedChanges
                _uiEvents.send(UiEvent.ShowSnackbar("已补全 $addedCount 个缺失字段。视图已切换至“未翻译”"))
                filterState.value = FilterState.UNTRANSLATED
                regenerateEntries() // Refresh the list
            } else {
                 _uiEvents.send(UiEvent.ShowSnackbar("所有缺失字段已在暂存区。视图已切换至“未翻译”"))
                 filterState.value = FilterState.UNTRANSLATED
                 regenerateEntries() // Refresh the list
            }
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
