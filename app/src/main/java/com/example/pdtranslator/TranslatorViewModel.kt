package com.example.pdtranslator

import android.app.Application
import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
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
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipOutputStream
import com.example.pdtranslator.engine.TranslationEngineManager
import com.example.pdtranslator.engine.TranslationResult

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
  val isIdentical: Boolean = false,
  val isDeleted: Boolean = false,
  val isDiff: Boolean = false,
  val dictValue: String? = null,
  val dictSourceValue: String? = null
)

// A3: UI data class for DELETED view
data class DeletedItem(
  val key: String,
  val targetValue: String,
  val isStagedForDeletion: Boolean
)

// B-fix2: Network suggestion with requestId
data class NetworkSuggestionState(
  val entryKey: String,
  val requestId: Long,
  val results: List<TranslationResult>
)

data class LanguageData(val fileName: String, val properties: Properties)

data class LanguageGroup(
  val name: String,
  val languages: Map<String, LanguageData>
)

enum class FilterState { ALL, UNTRANSLATED, TRANSLATED, MODIFIED, MISSING, DIFF, DELETED }

enum class ThemeColor {
  DEFAULT, M3, GREEN, LAVENDER, MODERN, PIXEL_DUNGEON
}

// --- ViewModel ---

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    // Include both old and new ISO 639 codes for complete coverage
    private val isoLanguages: Set<String> by lazy {
      val base = java.util.Locale.getISOLanguages().toMutableSet()
      // Ensure modern BCP 47 codes are present (Java returns old codes)
      base.addAll(listOf("id", "he", "yi", "jv", "ro"))
      // Ensure legacy codes are present too
      base.addAll(listOf("in", "iw", "ji", "jw", "mo"))
      base
    }
    // Legacy ISO 639 codes that Locale.forLanguageTag() cannot parse (BCP 47 replacements)
    private val legacyLangCodes = mapOf(
      "in" to "id",  // Indonesian: in → id
      "iw" to "he",  // Hebrew: iw → he
      "ji" to "yi",  // Yiddish: ji → yi
      "jw" to "jv",  // Javanese: jw → jv
      "mo" to "ro",  // Moldavian: mo → ro
    )
  }

  private val app get() = getApplication<Application>()

  // --- Draft, TM, Dictionary & Engine ---
  private val draftManager = DraftManager(application)
  private val translationMemory = TranslationMemory(application)
  private val dictionaryManager = DictionaryManager(application)
  val engineManager = TranslationEngineManager(application)
  private var _autoSaveEnabled = true

  // B-fix2: requestId counter for network suggestions
  private val requestCounter = AtomicLong(0)
  private var networkQueryJob: Job? = null
  private val _networkSuggestion = MutableStateFlow<NetworkSuggestionState?>(null)
  val networkSuggestion = _networkSuggestion.asStateFlow()

  // --- Internal State ---
  private val _languageGroups = MutableStateFlow<List<LanguageGroup>>(emptyList())
  private val _allEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())
  private val _stagedChanges = MutableStateFlow<Map<String, String>>(emptyMap())
  private val _stagedDeletions = MutableStateFlow<Set<String>>(emptySet())
  private val _showAboutDialog = MutableStateFlow(false)
  private val _themeColor = MutableStateFlow(ThemeColor.PIXEL_DUNGEON)
  private val _isSearchCardVisible = MutableStateFlow(true)
  private val _missingEntriesCount = MutableStateFlow(0)
  private val _deletedEntriesCount = MutableStateFlow(0)
  private val _diffEntriesCount = MutableStateFlow(0)
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

  // --- Dictionary State ---
  private val _dictEntryCount = MutableStateFlow(0)
  val dictEntryCount = _dictEntryCount.asStateFlow()

  // --- Created Languages (for export even without staged changes) ---
  private val _createdLanguages = MutableStateFlow<Set<String>>(emptySet())

  // --- A3: Deleted Items for DELETED view ---
  private val _deletedItems = MutableStateFlow<List<DeletedItem>>(emptyList())
  val deletedItems = _deletedItems.asStateFlow()

  // --- UI Events Channel ---
  private val _uiEvents = Channel<UiEvent>()
  val uiEvents = _uiEvents.receiveAsFlow()

  // --- UI State Exposed as StateFlows ---
  val languageGroupNames = MutableStateFlow<List<String>>(emptyList())
  val availableLanguages = MutableStateFlow<List<String>>(emptyList())
  val displayEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())
  val stagedChanges = _stagedChanges.asStateFlow()
  val stagedDeletions = _stagedDeletions.asStateFlow()
  val isSearchCardVisible = _isSearchCardVisible.asStateFlow()
  val missingEntriesCount = _missingEntriesCount.asStateFlow()
  val deletedEntriesCount = _deletedEntriesCount.asStateFlow()
  val diffEntriesCount = _diffEntriesCount.asStateFlow()
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
  val translationProgress = MutableStateFlow(0f) // 0..1 progress ratio

  val isSaveEnabled = MutableStateFlow(false)
  val showAboutDialog = _showAboutDialog.asStateFlow()
  val themeColor = _themeColor.asStateFlow()

  init {
    viewModelScope.launch(Dispatchers.Default) {
      combine(
        _allEntries, searchQuery, isCaseSensitive, isExactMatch,
        filterState, currentPage, _stagedChanges, _stagedDeletions
      ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val entries = flows[0] as List<TranslationEntry>
        val search = flows[1] as String
        val caseSensitive = flows[2] as Boolean
        val exactMatch = flows[3] as Boolean
        val filter = flows[4] as FilterState
        val page = flows[5] as Int
        val staged = flows[6] as Map<String, String>
        val deletions = flows[7] as Set<String>

        val processedEntries = entries.map { entry ->
          val isDeletedAndStaged = deletions.contains(entry.key)
          staged[entry.key]?.let { stagedValue ->
            val modified = stagedValue != entry.originalTargetValue
            entry.copy(
              targetValue = stagedValue,
              isModified = modified,
              isDeleted = entry.isDeleted && !isDeletedAndStaged
            )
          } ?: entry.copy(
            isModified = false,
            targetValue = entry.originalTargetValue,
            isDeleted = entry.isDeleted && !isDeletedAndStaged
          )
        }

        val filtered = processedEntries.filter { entry ->
          // DELETED filter is handled separately via deletedItems
          val matchesFilter = when (filter) {
            FilterState.ALL -> !entry.isDeleted
            FilterState.UNTRANSLATED -> (entry.isUntranslated || (entry.isMissing && staged.containsKey(entry.key))) && !entry.isDeleted
            FilterState.TRANSLATED -> !entry.isUntranslated && !entry.isMissing && !entry.isDeleted
            FilterState.MODIFIED -> entry.isModified && !entry.isDeleted
            FilterState.MISSING -> entry.isMissing && !staged.containsKey(entry.key) && !entry.isDeleted
            FilterState.DIFF -> entry.isDiff && !entry.isDeleted
            FilterState.DELETED -> entry.isDeleted
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

        val total = entries.count { !it.isDeleted }
        val translated = total - entries.count { it.isUntranslated && !it.isDeleted }
        val progress = if (total == 0) 0f else translated.toFloat() / total
        translationProgress.value = progress
        val pct = "%.2f".format(progress * 100)
        infoBarText.value = app.getString(R.string.translator_translation_progress, pct, total)
      }.collect {}
    }

    // A6: isSaveEnabled uses hasEffectiveChanges()
    viewModelScope.launch {
      combine(_stagedChanges, _stagedDeletions, _createdLanguages) { staged, deletions, created ->
        Triple(staged, deletions, created)
      }.collect {
        isSaveEnabled.value = hasEffectiveChanges() || _stagedDeletions.value.isNotEmpty() || _createdLanguages.value.isNotEmpty()
      }
    }

    // Auto-save: debounce staged changes, deletions, and created languages
    viewModelScope.launch {
      @OptIn(kotlinx.coroutines.FlowPreview::class)
      combine(_stagedChanges, _stagedDeletions, _createdLanguages) { staged, deletions, created ->
        Triple(staged, deletions, created)
      }
        .debounce(3000)
        .collectLatest { (staged, deletions, created) ->
          if (_autoSaveEnabled && (hasEffectiveChanges() || deletions.isNotEmpty() || created.isNotEmpty())) {
            autoSaveDraft()
          }
        }
    }

    // Load TM and Dictionary on start
    viewModelScope.launch {
      translationMemory.load()
    }
    viewModelScope.launch {
      dictionaryManager.load()
      _dictEntryCount.value = dictionaryManager.getTotalCount()
    }

    // Check for existing draft
    viewModelScope.launch {
      val draft = draftManager.load()
      if (draft != null) {
        _draftData.value = draft
      }
    }
  }

  // A6: Check if staged changes have any effective difference from original
  private fun hasEffectiveChanges(): Boolean {
    val staged = _stagedChanges.value
    if (staged.isEmpty()) return false
    val group = _languageGroups.value.find { it.name == selectedGroupName.value } ?: return false
    val targetCode = targetLangCode.value ?: return false
    val targetProps = group.languages[targetCode]?.properties ?: Properties()

    return staged.any { (key, value) ->
      val originalValue = targetProps.getProperty(key, "")
      val existsInTarget = targetProps.containsKey(key)
      value != originalValue || !existsInTarget
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

  /** Replace all occurrences of searchQuery in target values of displayed entries with replaceQuery */
  fun replaceAllMatching(): Int {
    val search = searchQuery.value
    val replace = replaceQuery.value
    if (search.isBlank()) return 0
    val caseSensitive = isCaseSensitive.value
    val exact = isExactMatch.value
    val entries = _allEntries.value
    var count = 0

    _stagedChanges.update { currentStaged ->
      val newStaged = currentStaged.toMutableMap()
      for (entry in entries) {
        if (entry.isDeleted) continue
        val current = newStaged[entry.key] ?: entry.targetValue
        if (current.isBlank()) continue

        val matches = if (exact) {
          current.equals(search, ignoreCase = !caseSensitive)
        } else {
          current.contains(search, ignoreCase = !caseSensitive)
        }

        if (matches) {
          val replaced = if (exact) {
            replace
          } else if (caseSensitive) {
            current.replace(search, replace)
          } else {
            current.replace(Regex(Regex.escape(search), RegexOption.IGNORE_CASE), replace)
          }
          newStaged[entry.key] = replaced
          count++
        }
      }
      newStaged
    }

    if (count > 0) regenerateEntries()
    return count
  }

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

      val parts = mutableListOf<String>()
      if (successCount > 0) parts.add(app.getString(R.string.import_success_count, successCount))
      if (overwriteCount > 0) parts.add(app.getString(R.string.import_overwrite_count, overwriteCount))
      if (ignoreCount > 0) parts.add(app.getString(R.string.import_ignore_count, ignoreCount))
      if (failCount > 0) parts.add(app.getString(R.string.import_fail_count, failCount))
      if (parts.isNotEmpty()) {
        _uiEvents.send(UiEvent.ShowSnackbar(parts.joinToString(", ")))
      }

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
                } catch (_: Exception) {}
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
        val content = String(rawBytes, Charsets.UTF_8)
        val props = loadProperties(content)
        val langMap = groups.getOrPut(baseName) { mutableMapOf() }
        val wasOverwrite = langMap.containsKey(langCode)
        langMap[langCode] = LanguageData(fileName, props)
        if (wasOverwrite) LoadResult.OVERWRITE else LoadResult.SUCCESS
      } ?: LoadResult.FAIL
    } catch (e: Exception) {
      Log.e("TranslatorVM", "Failed to load $fileName", e)
      LoadResult.FAIL
    }
  }

  fun selectGroup(name: String) {
    selectedGroupName.value = name
    sourceLangCode.value = null
    targetLangCode.value = null
    _allEntries.value = emptyList()
    _stagedChanges.value = emptyMap()
    _stagedDeletions.value = emptySet()
    _createdLanguages.value = emptySet()
    _deletedItems.value = emptyList()
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

  // --- Deletion Functions ---

  fun stageDeleteEntry(key: String) {
    _stagedDeletions.update { it + key }
    // A3: Update deletedItems to reflect staged status
    _deletedItems.update { items ->
      items.map { if (it.key == key) it.copy(isStagedForDeletion = true) else it }
    }
  }

  // A3: Unstage a deletion
  fun unstageDeleteEntry(key: String) {
    _stagedDeletions.update { it - key }
    _deletedItems.update { items ->
      items.map { if (it.key == key) it.copy(isStagedForDeletion = false) else it }
    }
  }

  fun deleteAllDeletedEntries() {
    viewModelScope.launch {
      val items = _deletedItems.value
      val unstagedKeys = items.filter { !it.isStagedForDeletion }.map { it.key }.toSet()
      _stagedDeletions.update { it + unstagedKeys }
      _deletedItems.update { items ->
        items.map { it.copy(isStagedForDeletion = true) }
      }
      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.deleted_all_done, unstagedKeys.size)))
    }
  }

  // --- Dictionary Functions ---

  fun saveToDictionary() {
    viewModelScope.launch(Dispatchers.IO) {
      val sourceCode = sourceLangCode.value ?: return@launch
      val targetCode = targetLangCode.value ?: return@launch
      val groupName = selectedGroupName.value ?: return@launch
      val group = _languageGroups.value.find { it.name == groupName } ?: return@launch

      val sourceProps = group.languages[sourceCode]?.properties ?: return@launch
      val targetProps = group.languages[targetCode]?.properties ?: Properties()

      // Merge staged changes into target
      val mergedTarget = Properties()
      mergedTarget.putAll(targetProps)
      _stagedChanges.value.forEach { (k, v) -> if (v.isNotBlank()) mergedTarget.setProperty(k, v) }

      // A1: pass groupName
      val count = dictionaryManager.importFromProperties(sourceProps, mergedTarget, groupName, sourceCode, targetCode)
      dictionaryManager.save()
      _dictEntryCount.value = dictionaryManager.getTotalCount()

      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_save_done, count)))
    }
  }

  fun applyDictionary() {
    viewModelScope.launch(Dispatchers.Default) {
      val sourceCode = sourceLangCode.value ?: return@launch
      val targetCode = targetLangCode.value ?: return@launch
      val groupName = selectedGroupName.value ?: return@launch
      val entries = _allEntries.value

      // A1: pass groupName
      val applied = dictionaryManager.applyToEntries(entries, groupName, sourceCode, targetCode)
      if (applied.isEmpty()) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_apply_none)))
        return@launch
      }

      _stagedChanges.update { current ->
        val newStaged = current.toMutableMap()
        applied.forEach { (k, v) -> newStaged[k] = v }
        newStaged
      }
      regenerateEntries()

      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_apply_done, applied.size)))
    }
  }

  fun clearDictionary() {
    viewModelScope.launch {
      dictionaryManager.clear()
      _dictEntryCount.value = 0
      regenerateEntries()
      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_clear_done)))
    }
  }

  // --- Custom Language Creation ---

  // A4: Language code validation + normalization
  fun createLanguage(langCode: String, copyFromLang: String? = null) {
    viewModelScope.launch {
      val groupName = selectedGroupName.value
      if (groupName == null) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.create_lang_no_group)))
        return@launch
      }

      val group = _languageGroups.value.find { it.name == groupName }
      if (group == null) return@launch

      // A4: Validate language code (with legacy code support)
      val legacyResolved = legacyLangCodes[langCode.lowercase()] ?: langCode
      val locale = java.util.Locale.forLanguageTag(legacyResolved)
      if (locale.language.isEmpty() || locale.language == "und" ||
          (!isoLanguages.contains(locale.language) && !isoLanguages.contains(langCode.lowercase()))) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.create_lang_invalid, langCode)))
        return@launch
      }

      // A4: Normalize
      val normalizedCode = locale.toLanguageTag()

      if (group.languages.containsKey(normalizedCode)) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.create_lang_exists, normalizedCode)))
        return@launch
      }

      val newFileName = "${groupName}_${normalizedCode}.properties"
      val newProps = Properties()

      // Copy entries from source language if specified
      if (copyFromLang != null) {
        val sourceData = group.languages[copyFromLang]
        if (sourceData != null) {
          newProps.putAll(sourceData.properties)
        }
      }

      val newLanguages = group.languages.toMutableMap()
      newLanguages[normalizedCode] = LanguageData(newFileName, newProps)
      val newGroup = group.copy(languages = newLanguages)

      _languageGroups.update { groups ->
        groups.map { if (it.name == groupName) newGroup else it }
      }
      availableLanguages.value = newGroup.languages.keys.sorted()
      _createdLanguages.update { it + normalizedCode }

      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.create_lang_done, normalizedCode)))
    }
  }

  // --- Save (optimized: only modified files) ---

  // A2: Export with proper error handling
  fun saveChangesToZip(resolver: ContentResolver, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        _autoSaveEnabled = false

        val groupName = selectedGroupName.value ?: return@launch
        val group = _languageGroups.value.find { it.name == groupName } ?: return@launch
        val targetCode = targetLangCode.value ?: return@launch
        val sourceCode = sourceLangCode.value ?: return@launch

        val staged = _stagedChanges.value
        val deletions = _stagedDeletions.value
        val createdLangs = _createdLanguages.value

        // A2: Check outputStream is not null
        val outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.export_fail_no_stream)))
          return@launch
        }

        // A2: Wrap ZIP writing in try/catch
        val zipSuccess = try {
          outputStream.use { os ->
            ZipOutputStream(os).use { zos ->
              group.languages.forEach { (langCode, langData) ->
                val isTarget = langCode == targetCode
                val isNewlyCreated = createdLangs.contains(langCode)

                if (isTarget) {
                  val effectiveStaged = staged.filter { (key, value) ->
                    val originalValue = langData.properties.getProperty(key, "")
                    val existsInTarget = langData.properties.containsKey(key)
                    value != originalValue || !existsInTarget
                  }

                  if (effectiveStaged.isNotEmpty() || deletions.isNotEmpty() || isNewlyCreated) {
                    val finalProps = Properties()
                    finalProps.putAll(langData.properties)
                    effectiveStaged.forEach { (key, value) ->
                      finalProps.setProperty(key, value)
                    }
                    deletions.forEach { key -> finalProps.remove(key) }

                    zos.putNextEntry(ZipEntry(langData.fileName))
                    val writer = OutputStreamWriter(zos, "UTF-8")
                    finalProps.store(writer, "PDTranslator Modified File")
                    writer.flush()
                    zos.closeEntry()
                  }
                } else if (isNewlyCreated) {
                  val finalProps = Properties()
                  finalProps.putAll(langData.properties)

                  zos.putNextEntry(ZipEntry(langData.fileName))
                  val writer = OutputStreamWriter(zos, "UTF-8")
                  finalProps.store(writer, "PDTranslator Modified File")
                  writer.flush()
                  zos.closeEntry()
                }
              }
            }
          }
          true
        } catch (e: Exception) {
          Log.e("TranslatorVM", "ZIP write failed", e)
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.export_fail_write)))
          false
        }

        if (!zipSuccess) return@launch

        // Record to TM (only after successful ZIP write)
        try {
          val sourceProps = group.languages[sourceCode]?.properties
          val targetProps = group.languages[targetCode]?.properties
          if (sourceProps != null && targetProps != null) {
            val sourceMap = mutableMapOf<String, String>()
            val targetMap = mutableMapOf<String, String>()
            sourceProps.forEach { (k, v) -> sourceMap[k as String] = v as String }
            targetProps.forEach { (k, v) -> targetMap[k as String] = v as String }
            staged.forEach { (k, v) -> targetMap[k] = v }
            translationMemory.importFromEntries(sourceMap, targetMap, sourceCode, targetCode)
            translationMemory.save()
          }
        } catch (e: Exception) {
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.tm_record_fail)))
        }

        // Delete draft (only after successful write)
        draftManager.delete()
        _draftData.value = null
        _draftValidation.value = DraftValidation.NO_DRAFT

        _stagedChanges.value = emptyMap()
        _stagedDeletions.value = emptySet()
        _createdLanguages.value = emptySet()
        regenerateEntries()

        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.export_success)))
      } finally {
        _autoSaveEnabled = true
      }
    }
  }

  // --- Draft Functions ---

  // A5: autoSaveDraft with contentDigest
  private suspend fun autoSaveDraft() {
    val groupName = selectedGroupName.value ?: return
    val srcLang = sourceLangCode.value ?: return
    val tgtLang = targetLangCode.value ?: return
    val staged = _stagedChanges.value
    val deletions = _stagedDeletions.value
    val createdLangs = _createdLanguages.value
    if (staged.isEmpty() && deletions.isEmpty() && createdLangs.isEmpty()) return

    val entries = _allEntries.value
    val keys = entries.map { it.key }
    val digest = DraftManager.computeKeysDigest(keys)

    // A5: Compute contentDigest (includes source text)
    val contentDigest = DraftManager.computeContentDigest(entries.map { it.key to it.sourceValue })

    // Serialize created language properties for persistence
    val createdLangData: Map<String, Map<String, String>>? = if (createdLangs.isNotEmpty()) {
      val group = _languageGroups.value.find { it.name == groupName }
      createdLangs.associateWith { langCode ->
        val props = group?.languages?.get(langCode)?.properties
        val map = mutableMapOf<String, String>()
        props?.forEach { (k, v) -> map[k as String] = v as String }
        map
      }
    } else null

    val draft = DraftData(
      groupName = groupName,
      sourceLangCode = srcLang,
      targetLangCode = tgtLang,
      stagedChanges = staged,
      highlightKeywords = _highlightKeywords.value,
      entryCount = entries.size,
      keysDigest = digest,
      timestamp = System.currentTimeMillis(),
      stagedDeletions = deletions,
      createdLanguages = createdLangData,
      contentDigest = contentDigest
    )
    draftManager.save(draft)
  }

  // A5: checkDraftAfterLoad with contentDigest
  private suspend fun checkDraftAfterLoad() {
    val draft = draftManager.load() ?: run {
      _draftData.value = null
      _draftValidation.value = DraftValidation.NO_DRAFT
      return
    }
    _draftData.value = draft

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
    val contentEntries = allKeys.map { key -> key to (srcProps.getProperty(key, "") as String) }
    val contentDigest = DraftManager.computeContentDigest(contentEntries)
    _draftValidation.value = DraftManager.validate(draft, allKeys.size, digest, contentDigest)
  }

  fun restoreDraft() {
    val draft = _draftData.value ?: return
    viewModelScope.launch {
      selectGroup(draft.groupName)

      // Restore created languages before selecting source/target
      draft.createdLanguages?.forEach { (langCode, propsMap) ->
        val group = _languageGroups.value.find { it.name == draft.groupName } ?: return@forEach
        if (!group.languages.containsKey(langCode)) {
          val newFileName = "${draft.groupName}_${langCode}.properties"
          val props = Properties()
          propsMap.forEach { (k, v) -> props.setProperty(k, v) }
          val newLanguages = group.languages.toMutableMap()
          newLanguages[langCode] = LanguageData(newFileName, props)
          val newGroup = group.copy(languages = newLanguages)
          _languageGroups.update { groups ->
            groups.map { if (it.name == draft.groupName) newGroup else it }
          }
          availableLanguages.value = newGroup.languages.keys.sorted()
        }
      }
      _createdLanguages.value = draft.createdLanguages?.keys?.toSet() ?: emptySet()

      selectSourceLanguage(draft.sourceLangCode)
      selectTargetLanguage(draft.targetLangCode)

      _stagedChanges.value = draft.stagedChanges
      _stagedDeletions.value = draft.stagedDeletions ?: emptySet()
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
      delay(200)
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

  // --- Translation Engine Functions ---

  // B-fix2: Translate with requestId to avoid stale results
  fun translateEntry(key: String, sourceText: String) {
    networkQueryJob?.cancel()
    val thisRequestId = requestCounter.incrementAndGet()
    networkQueryJob = viewModelScope.launch {
      val srcLang = sourceLangCode.value ?: return@launch
      val tgtLang = targetLangCode.value ?: return@launch

      // B-fix1: Use base language override if set
      val groupName = selectedGroupName.value ?: return@launch
      val overrideLang = engineManager.getBaseLangOverride(groupName)
      val effectiveSrcLang = if (overrideLang.isNotBlank()) overrideLang else srcLang

      val result = withContext(Dispatchers.IO) {
        engineManager.translate(sourceText, effectiveSrcLang, tgtLang)
      }

      // Only update if this is still the latest request
      if (requestCounter.get() == thisRequestId) {
        _networkSuggestion.value = NetworkSuggestionState(
          entryKey = key,
          requestId = thisRequestId,
          results = if (result.isSuccess) listOf(result.getOrThrow()) else emptyList()
        )
      }
    }
  }

  fun clearNetworkSuggestion() {
    networkQueryJob?.cancel()
    _networkSuggestion.value = null
  }

  fun applyNetworkSuggestion(key: String, translatedText: String) {
    stageChange(key, translatedText)
    _networkSuggestion.value = null
  }

  fun translateBatch(keys: List<String>, sourceTexts: List<String>) {
    viewModelScope.launch {
      val srcLang = sourceLangCode.value ?: return@launch
      val tgtLang = targetLangCode.value ?: return@launch
      val groupName = selectedGroupName.value ?: return@launch
      val overrideLang = engineManager.getBaseLangOverride(groupName)
      val effectiveSrcLang = if (overrideLang.isNotBlank()) overrideLang else srcLang

      val result = withContext(Dispatchers.IO) {
        engineManager.translateBatch(sourceTexts, effectiveSrcLang, tgtLang)
      }

      if (result.isSuccess) {
        val translations = result.getOrThrow()
        translations.forEachIndexed { index, tr ->
          if (index < keys.size && tr.translatedText.isNotBlank()) {
            stageChange(keys[index], tr.translatedText)
          }
        }
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.engine_batch_done, translations.size)))
      } else {
        val friendly = engineManager.getFriendlyError(engineManager.getSelectedEngineId(), result.exceptionOrNull())
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.engine_translate_fail, friendly)))
      }
    }
  }

  fun testEngineConnection() {
    viewModelScope.launch {
      val result = withContext(Dispatchers.IO) {
        engineManager.testConnection()
      }
      val selectedId = engineManager.getSelectedEngineId()
      if (result.isSuccess) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.engine_test_success, result.getOrThrow())))
      } else {
        val friendly = engineManager.getFriendlyError(selectedId, result.exceptionOrNull())
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.engine_test_fail, friendly)))
      }
    }
  }

  // --- Internal Functions ---

  private suspend fun processLoadedGroups(groups: Map<String, Map<String, LanguageData>>) {
    withContext(Dispatchers.Main) {
      _languageGroups.value = groups.map { (name, languages) -> LanguageGroup(name, languages) }.sortedBy { it.name }
      languageGroupNames.value = _languageGroups.value.map { it.name }

      val groupList = _languageGroups.value
      if (groupList.size == 1) {
        val group = groupList.first()
        selectedGroupName.value = group.name
        availableLanguages.value = group.languages.keys.sorted()

        val langs = availableLanguages.value
        if (langs.size == 2) {
          val sourceIdx = langs.indexOfFirst { it == "base" }.takeIf { it >= 0 } ?: 0
          val targetIdx = if (sourceIdx == 0) 1 else 0
          sourceLangCode.value = langs[sourceIdx]
          targetLangCode.value = langs[targetIdx]
          _stagedChanges.value = emptyMap()
          _stagedDeletions.value = emptySet()
          regenerateEntries()
        } else {
          sourceLangCode.value = null
          targetLangCode.value = null
          _allEntries.value = emptyList()
          _stagedChanges.value = emptyMap()
          _stagedDeletions.value = emptySet()
        }
      } else {
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
    _stagedDeletions.value = emptySet()
    _createdLanguages.value = emptySet()
    _deletedItems.value = emptyList()
  }

  private fun regenerateEntries() {
    val sourceCode = sourceLangCode.value
    val targetCode = targetLangCode.value
    val groupName = selectedGroupName.value
    val group = _languageGroups.value.find { it.name == groupName }

    if (sourceCode == null || targetCode == null || group == null || groupName == null) {
      _allEntries.value = emptyList()
      _missingEntriesCount.value = 0
      _deletedEntriesCount.value = 0
      _diffEntriesCount.value = 0
      _deletedItems.value = emptyList()
      return
    }

    viewModelScope.launch(Dispatchers.Default) {
      val sourceProps = group.languages[sourceCode]?.properties ?: Properties()
      val targetProps = group.languages[targetCode]?.properties ?: Properties()

      val allKeys = (sourceProps.keys + targetProps.keys).mapNotNull { it as? String }.distinct()
      val stagedKeys = _stagedChanges.value.keys
      val sortedKeys = (allKeys + stagedKeys).distinct().sorted()

      var missingCount = 0
      var deletedCount = 0
      var diffCount = 0

      // A3: Collect deleted items for the DELETED view
      val deletedItemList = mutableListOf<DeletedItem>()

      val newEntries = sortedKeys.map { key ->
        val sourceValue = sourceProps.getProperty(key, "")
        val isMissingInFile = !targetProps.containsKey(key) && sourceProps.containsKey(key)
        val isDeletedEntry = targetProps.containsKey(key) && !sourceProps.containsKey(key)
        val isStaged = _stagedChanges.value.containsKey(key)
        val isStagedDeletion = _stagedDeletions.value.contains(key)

        val originalTargetValue = if (!targetProps.containsKey(key)) "" else targetProps.getProperty(key, "")
        val finalTargetValue = _stagedChanges.value[key] ?: originalTargetValue

        val isModified = isStaged && (finalTargetValue != originalTargetValue)

        if (isMissingInFile && !isStaged) missingCount++
        if (isDeletedEntry && !isStagedDeletion) deletedCount++

        // A3: Build deleted items list (includes both staged and unstaged)
        if (isDeletedEntry) {
          deletedItemList.add(DeletedItem(key, targetProps.getProperty(key, ""), isStagedDeletion))
        }

        // A1: pass groupName to dictionary lookup
        val dictEntry = dictionaryManager.getEntry(groupName, sourceCode, targetCode, key)
        val isDiff = dictEntry != null
            && dictEntry.sourceText != null
            && sourceValue.isNotBlank()
            && dictEntry.sourceText != sourceValue
            && !isDeletedEntry
        if (isDiff) diffCount++

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
          isIdentical = isIdentical,
          isDeleted = isDeletedEntry && !isStagedDeletion,
          isDiff = isDiff,
          dictValue = dictEntry?.translation,
          dictSourceValue = dictEntry?.sourceText
        )
      }
      _allEntries.value = newEntries
      _missingEntriesCount.value = missingCount
      _deletedEntriesCount.value = deletedCount
      _diffEntriesCount.value = diffCount
      _deletedItems.value = deletedItemList
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
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.fill_missing_none)))
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

  private fun loadProperties(content: String): Properties {
    val props = Properties()
    try {
      props.load(StringReader(content))
    } catch (_: IllegalArgumentException) {
      val sanitized = content.replace(Regex("""\x5Cu(?![0-9a-fA-F]{4})"""), "\\\\u")
      props.load(StringReader(sanitized))
    }
    return props
  }

  private fun parseFileName(fileName: String): Pair<String, String> {
    val nameWithoutExt = fileName.substringBeforeLast('.')

    if (nameWithoutExt.endsWith("_chk", ignoreCase = true) || nameWithoutExt.endsWith("-chk", ignoreCase = true)) {
      val sep = if (nameWithoutExt.contains("_chk", ignoreCase = true)) "_chk" else "-chk"
      val base = nameWithoutExt.substringBeforeLast(sep, "")
      return if (base.isNotEmpty()) Pair(base, "zh-TW") else Pair(nameWithoutExt, "base")
    }

    val tokens = nameWithoutExt.split(Regex("[-_]"))
    if (tokens.size < 2) return Pair(nameWithoutExt, "base")

    for (take in minOf(3, tokens.size - 1) downTo 1) {
      val candidate = tokens.takeLast(take).joinToString("-")
      val candidateLower = candidate.lowercase()
      // Handle legacy ISO 639 codes (e.g., "in" → "id" for Indonesian)
      val isLegacy = legacyLangCodes.containsKey(candidateLower)
      val normalized = legacyLangCodes[candidateLower] ?: candidate
      val locale = java.util.Locale.forLanguageTag(normalized)
      if (locale.language.isNotEmpty() && locale.language != "und" &&
          (isoLanguages.contains(locale.language) || isoLanguages.contains(candidateLower))) {
        val baseName = reconstructBaseName(nameWithoutExt, take)
        if (baseName.isNotEmpty()) {
          // For legacy codes, use the original suffix as langCode to keep file matching consistent
          val langCode = if (isLegacy) candidateLower else locale.toLanguageTag()
          return Pair(baseName, langCode)
        }
      }
    }

    return Pair(nameWithoutExt, "base")
  }

  private fun reconstructBaseName(nameWithoutExt: String, suffixTokenCount: Int): String {
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
