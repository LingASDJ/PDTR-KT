package com.example.pdtranslator

import android.app.Application
import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
  val originalTargetValue: String,
  val isUntranslated: Boolean,
  var isModified: Boolean = false,
  val isMissing: Boolean = false,
  val isIdentical: Boolean = false
)

data class LanguageData(val fileName: String, val properties: Properties)

data class LanguageGroup(
  val name: String,
  val languages: Map<String, LanguageData>
)

enum class FilterState { ALL, UNTRANSLATED, TRANSLATED, MODIFIED, MISSING }

enum class ThemeColor {
  DEFAULT, M3, GREEN, LAVENDER, MODERN, PIXEL_DUNGEON
}

// --- ViewModel ---

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    // Cache ISO 639 language codes for parseFileName validation
    private val isoLanguages: Set<String> by lazy {
      java.util.Locale.getISOLanguages().toSet()
    }
  }

  // --- Draft & TM ---
  private val draftManager = DraftManager(application)
  private val translationMemory = TranslationMemory(application)
  private var _autoSaveEnabled = true

  // --- Internal State ---
  private val _languageGroups = MutableStateFlow<List<LanguageGroup>>(emptyList())
  private val _allEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())
  private val _stagedChanges = MutableStateFlow<Map<String, String>>(emptyMap())
  private val _showAboutDialog = MutableStateFlow(false)
  private val _themeColor = MutableStateFlow(ThemeColor.DEFAULT)
  private val _isSearchCardVisible = MutableStateFlow(true)
  private val _missingEntriesCount = MutableStateFlow(0)
  private val _highlightKeywords = MutableStateFlow<Set<String>>(emptySet())

  // --- Draft State ---
  private val _draftData = MutableStateFlow<DraftData?>(null)
  val draftData = _draftData.asStateFlow()
  private val _draftValidation = MutableStateFlow(DraftValidation.NO_DRAFT)
  val draftValidation = _draftValidation.asStateFlow()

  // --- TM State ---
  private val _tmSuggestions = MutableStateFlow<List<TmSuggestion>>(emptyList())
  val tmSuggestions = _tmSuggestions.asStateFlow()
  private var tmQueryJob: Job? = null

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
  val highlightKeywords = _highlightKeywords.asStateFlow()

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

        totalPages.value = (filtered.size + pageSize - 1) / pageSize.coerceAtLeast(1)
        val newPage = page.coerceIn(1, totalPages.value.coerceAtLeast(1))
        if (page != newPage) currentPage.value = newPage

        displayEntries.value = filtered.chunked(pageSize).getOrElse(newPage - 1) { emptyList() }

        if (filter == FilterState.UNTRANSLATED) {
          val total = entries.size
          val translated = total - entries.count { it.isUntranslated }
          val progress = if (total == 0) 0f else translated.toFloat() / total
          val app = getApplication<Application>()
          infoBarText.value = app.getString(R.string.translator_translation_progress, (progress * 100).toInt())
        } else {
          val app = getApplication<Application>()
          infoBarText.value = app.getString(R.string.translator_progress_info, entries.size)
        }
      }.collect {}
    }

    viewModelScope.launch {
      _stagedChanges.collect {
        isSaveEnabled.value = it.isNotEmpty()
      }
    }

    // Auto-save: debounce staged changes
    viewModelScope.launch {
      @OptIn(kotlinx.coroutines.FlowPreview::class)
      _stagedChanges.debounce(3000).collectLatest { staged ->
        if (_autoSaveEnabled && staged.isNotEmpty()) {
          autoSaveDraft()
        }
      }
    }

    // Load TM on start
    viewModelScope.launch {
      translationMemory.load()
    }

    // Check for existing draft
    viewModelScope.launch {
      val draft = draftManager.load()
      if (draft != null) {
        _draftData.value = draft
      }
    }
  }

  // --- Public Intent Functions ---

  fun addHighlightKeyword(keyword: String) {
    if (keyword.isNotBlank()) {
      _highlightKeywords.update { it + keyword.trim() }
    }
  }

  fun removeHighlightKeyword(keyword: String) {
    _highlightKeywords.update { it - keyword }
  }

  fun setSearchQuery(query: String) { searchQuery.value = query }
  fun setReplaceQuery(query: String) { replaceQuery.value = query }
  fun setCaseSensitive(isSensitive: Boolean) { isCaseSensitive.value = isSensitive }
  fun setExactMatch(isExact: Boolean) { isExactMatch.value = isExact }
  fun setFilter(filter: FilterState) {
    filterState.value = filter
    currentPage.value = 1
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
      var successCount = 0
      var ignoreCount = 0
      var overwriteCount = 0
      var failCount = 0

      uris.forEach { uri ->
        val fileName = getFileName(resolver, uri)
        if (fileName == null) {
          failCount++
          return@forEach
        }
        if (fileName.endsWith(".zip")) {
          val result = loadFromZip(resolver, uri, groups)
          successCount += result.first
          ignoreCount += result.second
          overwriteCount += result.third
        } else if (fileName.endsWith(".properties")) {
          val result = loadFile(resolver, uri, fileName, groups)
          when (result) {
            LoadResult.SUCCESS -> successCount++
            LoadResult.OVERWRITE -> { successCount++; overwriteCount++ }
            LoadResult.FAIL -> failCount++
            LoadResult.IGNORE -> ignoreCount++
          }
        } else {
          ignoreCount++
        }
      }

      processLoadedGroups(groups)

      // Build snackbar message
      val parts = mutableListOf<String>()
      if (successCount > 0) parts.add(getApplication<Application>().getString(R.string.import_success_count, successCount))
      if (overwriteCount > 0) parts.add(getApplication<Application>().getString(R.string.import_overwrite_count, overwriteCount))
      if (ignoreCount > 0) parts.add(getApplication<Application>().getString(R.string.import_ignore_count, ignoreCount))
      if (failCount > 0) parts.add(getApplication<Application>().getString(R.string.import_fail_count, failCount))
      if (parts.isNotEmpty()) {
        _uiEvents.send(UiEvent.ShowSnackbar(parts.joinToString(", ")))
      }

      // Validate existing draft against newly loaded files
      checkDraftAfterLoad()
    }
  }

  private enum class LoadResult { SUCCESS, OVERWRITE, FAIL, IGNORE }

  private fun loadFromZip(
    resolver: ContentResolver,
    uri: Uri,
    groups: MutableMap<String, MutableMap<String, LanguageData>>
  ): Triple<Int, Int, Int> {
    var success = 0
    var ignored = 0
    var overwritten = 0
    resolver.openInputStream(uri)?.use { zipInputStream ->
      ZipInputStream(zipInputStream).use { zis ->
        var zipEntry: ZipEntry?
        while (zis.nextEntry.also { zipEntry = it } != null) {
          zipEntry?.name?.let { entryName ->
            if (!entryName.endsWith("/")) {
              val innerFileName = entryName.substringAfterLast('/')
              if (!innerFileName.endsWith(".properties")) {
                ignored++
                return@let
              }
              val (baseName, langCode) = parseFileName(innerFileName)
              if (langCode != null) {
                try {
                  val content = BufferedReader(InputStreamReader(zis, Charsets.UTF_8)).readText()
                  val props = loadProperties(content)
                  val langMap = groups.getOrPut(baseName) { mutableMapOf() }
                  if (langMap.containsKey(langCode)) overwritten++
                  langMap[langCode] = LanguageData(entryName, props)
                  success++
                } catch (_: Exception) {
                  // count as success=0 for this entry, but don't increment fail
                  // zip internal entries that fail parsing are silently skipped
                }
              }
            }
          }
        }
      }
    }
    return Triple(success, ignored, overwritten)
  }

  private fun loadFile(
    resolver: ContentResolver,
    uri: Uri,
    fileName: String,
    groups: MutableMap<String, MutableMap<String, LanguageData>>
  ): LoadResult {
    val (baseName, langCode) = parseFileName(fileName)
    return try {
      resolver.openInputStream(uri)?.use { stream ->
        val rawBytes = stream.readBytes()
        // Try UTF-8 first (modern .properties with direct CJK characters),
        // fall back to ISO-8859-1 (traditional .properties with \uXXXX escapes)
        val content = String(rawBytes, Charsets.UTF_8)
        val props = loadProperties(content)
        val langMap = groups.getOrPut(baseName) { mutableMapOf() }
        val wasOverwrite = langMap.containsKey(langCode)
        langMap[langCode] = LanguageData(fileName, props)
        if (wasOverwrite) LoadResult.OVERWRITE else LoadResult.SUCCESS
      } ?: LoadResult.FAIL
    } catch (e: Exception) {
      android.util.Log.e("TranslatorVM", "Failed to load $fileName", e)
      LoadResult.FAIL
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
      try {
        _autoSaveEnabled = false

        val groupName = selectedGroupName.value ?: return@launch
        val group = _languageGroups.value.find { it.name == groupName } ?: return@launch
        val targetCode = targetLangCode.value ?: return@launch
        val sourceCode = sourceLangCode.value ?: return@launch

        val staged = _stagedChanges.value

        // Export ZIP
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

        // Record to TM (failure only shows Snackbar, does not block)
        try {
          val sourceProps = group.languages[sourceCode]?.properties
          val targetProps = group.languages[targetCode]?.properties
          if (sourceProps != null && targetProps != null) {
            val sourceMap = mutableMapOf<String, String>()
            val targetMap = mutableMapOf<String, String>()
            sourceProps.forEach { (k, v) -> sourceMap[k as String] = v as String }
            targetProps.forEach { (k, v) -> targetMap[k as String] = v as String }
            // Include staged changes in target
            staged.forEach { (k, v) -> targetMap[k] = v }
            translationMemory.importFromEntries(sourceMap, targetMap, sourceCode, targetCode)
            translationMemory.save()
          }
        } catch (e: Exception) {
          _uiEvents.send(UiEvent.ShowSnackbar(
            getApplication<Application>().getString(R.string.tm_record_fail)
          ))
        }

        // Delete draft
        draftManager.delete()
        _draftData.value = null
        _draftValidation.value = DraftValidation.NO_DRAFT

        _stagedChanges.value = emptyMap()
        regenerateEntries()

        _uiEvents.send(UiEvent.ShowSnackbar(
          getApplication<Application>().getString(R.string.export_success)
        ))
      } finally {
        _autoSaveEnabled = true
      }
    }
  }

  // --- Draft Functions ---

  private suspend fun autoSaveDraft() {
    val groupName = selectedGroupName.value ?: return
    val srcLang = sourceLangCode.value ?: return
    val tgtLang = targetLangCode.value ?: return
    val staged = _stagedChanges.value
    if (staged.isEmpty()) return

    val entries = _allEntries.value
    val keys = entries.map { it.key }
    val digest = DraftManager.computeKeysDigest(keys)

    val draft = DraftData(
      groupName = groupName,
      sourceLangCode = srcLang,
      targetLangCode = tgtLang,
      stagedChanges = staged,
      highlightKeywords = _highlightKeywords.value,
      entryCount = entries.size,
      keysDigest = digest,
      timestamp = System.currentTimeMillis()
    )
    draftManager.save(draft)
  }

  private suspend fun checkDraftAfterLoad() {
    val draft = draftManager.load() ?: run {
      _draftData.value = null
      _draftValidation.value = DraftValidation.NO_DRAFT
      return
    }
    _draftData.value = draft

    // Find the group to compute current keys digest
    val group = _languageGroups.value.find { it.name == draft.groupName }
    if (group == null) {
      _draftValidation.value = DraftValidation.MISMATCH
      return
    }
    val srcProps = group.languages[draft.sourceLangCode]?.properties
    val tgtProps = group.languages[draft.targetLangCode]?.properties
    if (srcProps == null || tgtProps == null) {
      _draftValidation.value = DraftValidation.MISMATCH
      return
    }
    val allKeys = (srcProps.keys + tgtProps.keys).mapNotNull { it as? String }.distinct().sorted()
    val digest = DraftManager.computeKeysDigest(allKeys)
    _draftValidation.value = DraftManager.validate(draft, allKeys.size, digest)
  }

  fun restoreDraft() {
    val draft = _draftData.value ?: return
    viewModelScope.launch {
      // Select group and languages
      selectGroup(draft.groupName)
      selectSourceLanguage(draft.sourceLangCode)
      selectTargetLanguage(draft.targetLangCode)

      // Restore staged changes
      _stagedChanges.value = draft.stagedChanges
      _highlightKeywords.value = draft.highlightKeywords

      regenerateEntries()

      _draftData.value = null
      _draftValidation.value = DraftValidation.NO_DRAFT
    }
  }

  fun discardDraft() {
    viewModelScope.launch {
      draftManager.delete()
      _draftData.value = null
      _draftValidation.value = DraftValidation.NO_DRAFT
    }
  }

  // --- TM Functions ---

  fun requestTmSuggestions(sourceText: String) {
    tmQueryJob?.cancel()
    tmQueryJob = viewModelScope.launch {
      delay(200) // debounce
      val srcLang = sourceLangCode.value ?: return@launch
      val tgtLang = targetLangCode.value ?: return@launch
      val results = withContext(Dispatchers.Default) {
        translationMemory.findMatches(sourceText, srcLang, tgtLang)
      }
      _tmSuggestions.value = results
    }
  }

  fun clearTmSuggestions() {
    tmQueryJob?.cancel()
    _tmSuggestions.value = emptyList()
  }

  fun applyTmSuggestion(key: String, targetText: String) {
    stageChange(key, targetText)
  }

  // --- Internal Functions ---

  private suspend fun processLoadedGroups(groups: Map<String, Map<String, LanguageData>>) {
    withContext(Dispatchers.Main) {
      _languageGroups.value = groups.map { (name, languages) -> LanguageGroup(name, languages) }.sortedBy { it.name }
      languageGroupNames.value = _languageGroups.value.map { it.name }

      // Auto-select if there's only one group
      val groupList = _languageGroups.value
      if (groupList.size == 1) {
        val group = groupList.first()
        selectedGroupName.value = group.name
        availableLanguages.value = group.languages.keys.sorted()

        // Auto-select languages if exactly 2 available
        val langs = availableLanguages.value
        if (langs.size == 2) {
          // Prefer "base" or the first alphabetically as source
          val sourceIdx = langs.indexOfFirst { it == "base" }.takeIf { it >= 0 } ?: 0
          val targetIdx = if (sourceIdx == 0) 1 else 0
          sourceLangCode.value = langs[sourceIdx]
          targetLangCode.value = langs[targetIdx]
          _stagedChanges.value = emptyMap()
          regenerateEntries()
        } else {
          // More than 2 languages — let user pick, but clear old selections
          sourceLangCode.value = null
          targetLangCode.value = null
          _allEntries.value = emptyList()
          _stagedChanges.value = emptyMap()
        }
      } else {
        // Multiple groups — reset and let user pick
        resetAllSelections()
      }
    }
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
        _uiEvents.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.fill_missing_none)))
        return@launch
      }

      val currentStaged = _stagedChanges.value
      val newStagedChanges = currentStaged.toMutableMap()
      var addedCount = 0

      missingKeys.forEach { key ->
        if (!currentStaged.containsKey(key)) {
          newStagedChanges[key] = ""
          addedCount++
        }
      }

      val app = getApplication<Application>()
      if (addedCount > 0) {
        _stagedChanges.value = newStagedChanges
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.fill_missing_done, addedCount)))
        filterState.value = FilterState.UNTRANSLATED
        regenerateEntries()
      } else {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.fill_missing_already_staged)))
        filterState.value = FilterState.UNTRANSLATED
        regenerateEntries()
      }
    }
  }

  /**
   * Safely load a .properties file content, handling malformed unicode escapes.
   * Properties.load() expects ISO 8859-1 encoding and throws IllegalArgumentException
   * on malformed \uXXXX sequences. This method sanitizes only the bad \u escapes.
   */
  private fun loadProperties(content: String): Properties {
    val props = Properties()
    try {
      props.load(StringReader(content))
    } catch (_: IllegalArgumentException) {
      // Sanitize malformed \uXXXX: \x5C = backslash in regex, avoids Java regex \u bug
      val sanitized = content.replace(Regex("""\x5Cu(?![0-9a-fA-F]{4})"""), "\\\\u")
      props.load(StringReader(sanitized))
    }
    return props
  }

  private fun parseFileName(fileName: String): Pair<String, String> {
    val nameWithoutExt = fileName.substringBeforeLast('.')

    // Special case: chk suffix
    if (nameWithoutExt.endsWith("_chk", ignoreCase = true) || nameWithoutExt.endsWith("-chk", ignoreCase = true)) {
      val sep = if (nameWithoutExt.contains("_chk", ignoreCase = true)) "_chk" else "-chk"
      val base = nameWithoutExt.substringBeforeLast(sep, "")
      return if (base.isNotEmpty()) Pair(base, "zh-TW") else Pair(nameWithoutExt, "base")
    }

    // Split by both _ and - to get all segments
    val tokens = nameWithoutExt.split(Regex("[-_]"))
    if (tokens.size < 2) return Pair(nameWithoutExt, "base")

    // Try candidate suffixes from longest (3 segments) to shortest (1 segment)
    // e.g. for [messages, zh, Hant, TW] try "zh-Hant-TW", then "Hant-TW", then "TW"
    for (take in minOf(3, tokens.size - 1) downTo 1) {
      val candidate = tokens.takeLast(take).joinToString("-")
      val locale = java.util.Locale.forLanguageTag(candidate)
      // Validate: language must be a real ISO 639 code, not just any 2-8 letter string
      if (locale.language.isNotEmpty() && isoLanguages.contains(locale.language)) {
        // Valid locale found — reconstruct base name from original string
        // Find the position in the original string where the locale suffix starts
        val baseName = reconstructBaseName(nameWithoutExt, take)
        if (baseName.isNotEmpty()) {
          // Normalize to BCP-47 format
          val langCode = locale.toLanguageTag()
          return Pair(baseName, langCode)
        }
      }
    }

    return Pair(nameWithoutExt, "base")
  }

  private fun reconstructBaseName(nameWithoutExt: String, suffixTokenCount: Int): String {
    // Walk from the end, skip `suffixTokenCount` separator-delimited segments
    var remaining = nameWithoutExt
    repeat(suffixTokenCount) {
      val lastSep = maxOf(remaining.lastIndexOf('_'), remaining.lastIndexOf('-'))
      if (lastSep <= 0) return ""
      remaining = remaining.substring(0, lastSep)
    }
    return remaining
  }

  private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
    return resolver.query(uri, null, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) cursor.getString(nameIndex) else null
      } else null
    }
  }

  fun getLanguageDisplayName(code: String, context: Context): String {
    return LanguageUtils.getDisplayName(code, context)
  }
}
