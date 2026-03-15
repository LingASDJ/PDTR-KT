package com.example.pdtranslator

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdtranslator.R
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
    val isUntranslated: Boolean,
    var isModified: Boolean = false,
    val isMissing: Boolean = false
)

data class LanguageData(val fileName: String, val properties: Properties)

data class LanguageGroup(
    val name: String,
    val languages: Map<String, LanguageData> // Key: langCode (e.g., "en", "base")
)

enum class FilterState { ALL, UNTRANSLATED, TRANSLATED, MODIFIED, MISSING }

class TranslatorViewModel : ViewModel() {

    // --- Internal State ---
    private val _languageGroups = MutableStateFlow<List<LanguageGroup>>(emptyList())
    private val _allEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())
    private val _modifiedEntries = MutableStateFlow<Map<String, Properties>>(emptyMap()) // Key: langCode, Value: Modified properties
    private val _showAboutDialog = MutableStateFlow(false)
    private val _translationEngine = MutableStateFlow(R.string.translation_engine_youdao)

    // --- UI State Exposed as StateFlows ---
    val languageGroupNames = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val displayEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())

    val selectedGroupName = MutableStateFlow<String?>(null)
    val sourceLangCode = MutableStateFlow<String?>(null)
    val targetLangCode = MutableStateFlow<String?>(null)
    
    val searchText = MutableStateFlow("")
    val filterState = MutableStateFlow(FilterState.ALL)

    val currentPage = MutableStateFlow(1)
    val pageSize = 20 // Can be configured in settings later
    val totalPages = MutableStateFlow(1)
    val translationProgress = MutableStateFlow(0f)
    val isSaveEnabled = MutableStateFlow(false)

    val showAboutDialog = _showAboutDialog.asStateFlow()
    val translationEngine = _translationEngine.asStateFlow()

    init {
        // This coroutine reacts to any state changes and updates the final displayed list.
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                _allEntries, 
                searchText, 
                filterState, 
                currentPage
            ) { entries, search, filter, page ->
                // Filtering logic
                val filtered = entries.filter { entry ->
                    val matchesFilter = when (filter) {
                        FilterState.ALL -> true
                        FilterState.UNTRANSLATED -> entry.isUntranslated
                        FilterState.TRANSLATED -> !entry.isUntranslated && !entry.isMissing
                        FilterState.MODIFIED -> entry.isModified
                        FilterState.MISSING -> entry.isMissing
                    }
                    val matchesSearch = if (search.isBlank()) true else {
                        entry.key.contains(search, ignoreCase = true) || 
                        entry.sourceValue.contains(search, ignoreCase = true)
                    }
                    matchesFilter && matchesSearch
                }

                // Pagination logic
                totalPages.value = (filtered.size + pageSize - 1) / pageSize.coerceAtLeast(1)
                if (page > totalPages.value) currentPage.value = 1

                val newPage = if (page > totalPages.value) 1 else page

                displayEntries.value = filtered.chunked(pageSize).getOrElse(newPage - 1) { emptyList() }

            }.collect {}
        }
    }

    // --- Public Intent Functions ---

    fun setShowAboutDialog(show: Boolean) {
        _showAboutDialog.value = show
    }

    fun setTranslationEngine(engine: Int) {
        _translationEngine.value = engine
        // Here you might add logic to re-initialize your translation service
    }

    fun loadFilesFromZip(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = mutableMapOf<String, MutableMap<String, LanguageData>>()
            resolver.openInputStream(uri)?.use {
                ZipInputStream(it).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val pathParts = entry.name.split('/').filter(String::isNotBlank)
                            if (pathParts.size >= 2) {
                                val groupName = pathParts.first()
                                val fileName = pathParts.last()
                                val (baseName, langCode) = parseFileName(fileName)

                                if (langCode != null) {
                                    val content = BufferedReader(InputStreamReader(zis)).readText()
                                    val props = Properties().apply { load(StringReader(content)) }
                                    groups.getOrPut(groupName) { mutableMapOf() }[langCode] = LanguageData(fileName, props)
                                }
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            processLoadedGroups(groups)
        }
    }

    fun loadFilesFromUris(resolver: ContentResolver, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = mutableMapOf<String, MutableMap<String, LanguageData>>()
            for (uri in uris) {
                val fileName = getFileName(resolver, uri) ?: continue
                val (baseName, langCode) = parseFileName(fileName)
                if (langCode != null) {
                    val content = resolver.openInputStream(uri)?.use { stream -> BufferedReader(InputStreamReader(stream)).readText() } ?: continue
                    val props = Properties().apply { load(StringReader(content)) }
                    groups.getOrPut(baseName) { mutableMapOf() }[langCode] = LanguageData(fileName, props)
                }
            }
            processLoadedGroups(groups)
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
                        finalProps.putAll(langData.properties) // Start with original
                        _modifiedEntries.value[langCode]?.let { mods -> finalProps.putAll(mods) } // Apply modifications

                        val entryPath = "${group.name}/${langData.fileName}"
                        zos.putNextEntry(ZipEntry(entryPath))
                        val writer = OutputStreamWriter(zos)
                        finalProps.store(writer, "PDTranslator Modified File")
                        writer.flush() // Don't close writer, as it would close the zos
                        zos.closeEntry()
                    }
                }
            }
            // Reset modified state for the saved group
            _modifiedEntries.update { currentMods ->
                val newMods = currentMods.toMutableMap()
                group.languages.keys.forEach { langCode -> newMods.remove(langCode) }
                newMods
            }
            isSaveEnabled.value = _modifiedEntries.value.isNotEmpty()
            regenerateEntries() // Refresh UI to show no more modified entries
        }
    }
    
    fun selectGroup(name: String) {
        selectedGroupName.value = name
        sourceLangCode.value = null
        targetLangCode.value = null
        _allEntries.value = emptyList()
        availableLanguages.value = _languageGroups.value.find { group -> group.name == name }?.languages?.keys?.sorted() ?: emptyList()
    }

    fun selectSourceLanguage(code: String) {
        sourceLangCode.value = code
        if (targetLangCode.value != null) regenerateEntries()
    }

    fun selectTargetLanguage(code: String) {
        targetLangCode.value = code
        if (sourceLangCode.value != null) regenerateEntries()
    }

    fun setSearchText(text: String) { searchText.value = text }
    fun setFilter(filter: FilterState) { filterState.value = filter }
    fun nextPage() { if (currentPage.value < totalPages.value) currentPage.value++ }
    fun previousPage() { if (currentPage.value > 1) currentPage.value-- }

    fun updateEntry(key: String, newTargetValue: String) {
        val langCode = targetLangCode.value ?: return

        // Update modification cache
        _modifiedEntries.update { currentMods ->
            val newMods = currentMods.toMutableMap()
            val langProps = newMods.getOrPut(langCode) { Properties() }
            langProps.setProperty(key, newTargetValue)
            newMods
        }

        // Update live entry for immediate UI feedback
        _allEntries.update { currentEntries ->
            currentEntries.map { entry -> 
                if (entry.key == key) entry.copy(targetValue = newTargetValue, isModified = true) else entry
            }
        }
        isSaveEnabled.value = true
    }

    fun completeMissingEntries() {
        val langCode = targetLangCode.value ?: return
        val missingEntries = _allEntries.value.filter { it.isMissing }
        if (missingEntries.isEmpty()) return

        // Update modification cache
        _modifiedEntries.update { currentMods ->
            val newMods = currentMods.toMutableMap()
            val langProps = newMods.getOrPut(langCode) { Properties() }
            missingEntries.forEach { entry ->
                // Add with an empty string, it will become an "untranslated" entry
                if (!langProps.containsKey(entry.key)) {
                    langProps.setProperty(entry.key, "")
                }
            }
            newMods
        }

        // Update live entries for immediate UI feedback
        _allEntries.update { currentEntries ->
            currentEntries.map { entry ->
                if (entry.isMissing) {
                    // No longer missing, now it is untranslated and modified (because we added it)
                    entry.copy(targetValue = "", isMissing = false, isUntranslated = true, isModified = true)
                } else {
                    entry
                }
            }
        }
        isSaveEnabled.value = true
        // After completing, switch filter to UNTRANSLATED to show the newly added entries
        filterState.value = FilterState.UNTRANSLATED
    }

    // --- Private Helper Functions ---

    private fun processLoadedGroups(groups: Map<String, Map<String, LanguageData>>) {
        _languageGroups.value = groups.map { (name, languages) -> LanguageGroup(name, languages) }
        languageGroupNames.value = _languageGroups.value.map { group -> group.name }.sorted()

        // Reset everything
        selectedGroupName.value = null
        sourceLangCode.value = null
        targetLangCode.value = null
        availableLanguages.value = emptyList() // Clear the list of available languages
        _allEntries.value = emptyList()
        _modifiedEntries.value = emptyMap()
        isSaveEnabled.value = false
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
            val modifiedProps = _modifiedEntries.value[targetCode]
            
            val sortedKeys = sourceProps.stringPropertyNames().sorted()

            val newEntries = sortedKeys.map { key ->
                val sourceValue = sourceProps.getProperty(key, "")
                
                val isMissing = !targetProps.containsKey(key) && modifiedProps?.containsKey(key) != true
                
                val originalTargetValue = if (isMissing) "" else targetProps.getProperty(key, "")
                val isModified = modifiedProps?.containsKey(key) ?: false
                val finalTargetValue = if (isModified) modifiedProps!!.getProperty(key) else originalTargetValue
                
                val isUntranslated = !isMissing && (finalTargetValue.isBlank() || (sourceValue == finalTargetValue && !isModified))

                TranslationEntry(
                    key = key,
                    sourceValue = sourceValue,
                    targetValue = finalTargetValue,
                    isUntranslated = isUntranslated,
                    isModified = isModified,
                    isMissing = isMissing
                )
            }

            _allEntries.value = newEntries

            val translatedCount = newEntries.count { entry -> !entry.isUntranslated && !entry.isMissing }
            translationProgress.value = if (newEntries.isEmpty()) 0f else translatedCount.toFloat() / newEntries.size
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
            Pair(base, "base") // "base" for files like actors.properties
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
