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

data class PendingDictionaryImport(
  val fileName: String,
  val bytes: ByteArray,
  val importedCount: Int,
  val conflictNames: List<String>
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
  private val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)

  // --- Draft, TM, Dictionary & Engine ---
  private val draftManager = DraftManager(application)
  private val translationMemory = TranslationMemory(application)
  private val dictionaryManager = DictionaryManager(application)
  val engineManager = TranslationEngineManager(application)
  private var _autoSaveEnabled = true

  // B-fix2: requestId counter for network suggestions
  private val requestCounter = AtomicLong(0)
  private var networkQueryJob: Job? = null
  private var regenerateJob: Job? = null
  private val dictionarySelectionRunner = LatestTaskRunner(viewModelScope)
  private val _networkSuggestion = MutableStateFlow<NetworkSuggestionState?>(null)
  val networkSuggestion = _networkSuggestion.asStateFlow()

  // --- Internal State ---
  private val _languageGroups = MutableStateFlow<List<LanguageGroup>>(emptyList())
  private val _allEntries = MutableStateFlow<List<TranslationEntry>>(emptyList())
  private val _stagedChanges = MutableStateFlow<Map<String, String>>(emptyMap())
  private val _stagedDeletions = MutableStateFlow<Set<String>>(emptySet())
  private val _scopedWorkspaceState = MutableStateFlow(ScopedWorkspaceState())
  private val _showAboutDialog = MutableStateFlow(false)
  private val _themeColor = MutableStateFlow(loadSavedThemeColor())
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
  private val _createLanguageSuccessSerial = MutableStateFlow(0L)
  val createLanguageSuccessSerial = _createLanguageSuccessSerial.asStateFlow()

  // --- TM State ---
  private val _tmSuggestions = MutableStateFlow<List<TmSuggestion>>(emptyList())
  val tmSuggestions = _tmSuggestions.asStateFlow()
  private var tmQueryJob: Job? = null

  // --- Dictionary State ---
  private val _dictEntryCount = MutableStateFlow(0)
  val dictEntryCount = _dictEntryCount.asStateFlow()
  val dictionaries = MutableStateFlow<List<NamedDictionary>>(emptyList())
  val selectedDictionaryId = MutableStateFlow<String?>(null)
  val selectedDictionaryName = MutableStateFlow("")
  val dictionaryCount = MutableStateFlow(0)
  val canDeleteDictionary = MutableStateFlow(false)
  val isDictionaryPreviewVisible = MutableStateFlow(false)
  val dictionaryPreviewQuery = MutableStateFlow("")
  val dictionaryPreviewEntries = MutableStateFlow<List<DictionaryPreviewItem>>(emptyList())
  val pendingDictionaryImport = MutableStateFlow<PendingDictionaryImport?>(null)

  // --- Created Languages (for export even without staged changes) ---
  private val _createdLanguages = MutableStateFlow<Set<String>>(emptySet())

  // --- A3: Deleted Items for DELETED view ---
  private val _deletedItems = MutableStateFlow<List<DeletedItem>>(emptyList())
  val deletedItems = _deletedItems.asStateFlow()

  // --- UI Events Channel ---
  private val _uiEvents = UiEventChannel()
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
  private val _searchResultKeys = MutableStateFlow<List<String>>(emptyList())
  val searchResultCount = MutableStateFlow(0)
  private val _currentSearchResultIndex = MutableStateFlow(-1)
  val currentSearchResultIndex = _currentSearchResultIndex.asStateFlow()
  val currentSearchResultKey = MutableStateFlow<String?>(null)

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

        val filteredByFilter = processedEntries.filter { entry ->
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
          matchesFilter
        }

        val searchMatches = if (search.isBlank()) {
          emptyList()
        } else {
          filteredByFilter.filter { entry ->
            SearchReplaceUtils.matches(
              entry = SearchableEntry(
                key = entry.key,
                sourceValue = entry.sourceValue,
                targetValue = staged[entry.key] ?: entry.targetValue
              ),
              query = search,
              caseSensitive = caseSensitive,
              exactMatch = exactMatch
            )
          }
        }
        val filtered = if (search.isBlank()) filteredByFilter else searchMatches

        _searchResultKeys.value = searchMatches.map { it.key }
        searchResultCount.value = searchMatches.size
        if (searchMatches.isEmpty()) {
          _currentSearchResultIndex.value = -1
          currentSearchResultKey.value = null
        } else {
          val normalizedIndex = _currentSearchResultIndex.value.coerceIn(0, searchMatches.lastIndex)
          if (_currentSearchResultIndex.value != normalizedIndex) {
            _currentSearchResultIndex.value = normalizedIndex
          }
          currentSearchResultKey.value = searchMatches[normalizedIndex].key
          val expectedPage = normalizedIndex / pageSize + 1
          if (search.isNotBlank() && currentPage.value != expectedPage) {
            currentPage.value = expectedPage
          }
        }

        totalPages.value = (filtered.size + pageSize - 1) / pageSize.coerceAtLeast(1)
        val newPage = page.coerceIn(1, totalPages.value.coerceAtLeast(1))
        if (page != newPage) currentPage.value = newPage

        displayEntries.value = filtered.chunked(pageSize).getOrElse(newPage - 1) { emptyList() }

        val progressState = TranslationProgressCalculator.calculate(
          processedEntries.map { entry ->
            ProgressEntry(
              sourceValue = entry.sourceValue,
              targetValue = entry.targetValue,
              isDeleted = entry.isDeleted
            )
          }
        )
        translationProgress.value = progressState.ratio
        val pct = "%.2f".format(progressState.ratio * 100)
        infoBarText.value = app.getString(R.string.translator_translation_progress, pct, progressState.totalCount)
      }.collect {}
    }

    // A6: isSaveEnabled considers all scoped workspace changes
    viewModelScope.launch {
      combine(_scopedWorkspaceState, _languageGroups) { workspace, groups ->
        hasPendingWorkspaceChanges(workspace, groups)
      }.collect { hasChanges ->
        isSaveEnabled.value = hasChanges
      }
    }

    // Auto-save: debounce workspace changes across all scopes
    viewModelScope.launch {
      @OptIn(kotlinx.coroutines.FlowPreview::class)
      _scopedWorkspaceState
        .debounce(3000)
        .collectLatest { workspace ->
          if (_autoSaveEnabled && hasPendingWorkspaceChanges(workspace, _languageGroups.value)) {
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
      refreshDictionaryState()
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
    return hasEffectiveChanges(activeEditScope(), _stagedChanges.value, _languageGroups.value)
  }

  private fun hasEffectiveChanges(
    scope: EditScope?,
    staged: Map<String, String>,
    groups: List<LanguageGroup>
  ): Boolean {
    if (scope == null || staged.isEmpty()) return false
    val group = groups.find { it.name == scope.groupName } ?: return false
    val targetProps = group.languages[scope.targetLangCode]?.properties ?: Properties()

    return staged.any { (key, value) ->
      val originalValue = targetProps.getProperty(key, "")
      val existsInTarget = targetProps.containsKey(key)
      value != originalValue || !existsInTarget
    }
  }

  private fun hasPendingWorkspaceChanges(
    workspace: ScopedWorkspaceState = _scopedWorkspaceState.value,
    groups: List<LanguageGroup> = _languageGroups.value
  ): Boolean {
    return workspace.stagedChangesByScope.any { (scope, staged) ->
      hasEffectiveChanges(scope, staged, groups)
    } ||
      workspace.stagedDeletionsByScope.values.any { it.isNotEmpty() } ||
      workspace.createdLanguagesByGroup.values.any { it.isNotEmpty() }
  }

  private fun primaryDraftScope(workspace: ScopedWorkspaceState): EditScope? {
    activeEditScope()?.let { return it }
    workspace.stagedChangesByScope.keys.firstOrNull()?.let { return it }
    workspace.stagedDeletionsByScope.keys.firstOrNull()?.let { return it }

    val groupName = workspace.createdLanguagesByGroup.keys.firstOrNull() ?: return null
    val group = _languageGroups.value.find { it.name == groupName } ?: return null
    val sourceCode = sourceLangCode.value?.takeIf { group.languages.containsKey(it) }
      ?: group.languages.keys.sorted().firstOrNull()
      ?: return null
    val targetCode = targetLangCode.value?.takeIf { group.languages.containsKey(it) && it != sourceCode }
      ?: workspace.createdLanguages(groupName).firstOrNull { it != sourceCode }
      ?: group.languages.keys.sorted().firstOrNull { it != sourceCode }
      ?: sourceCode

    return EditScope(groupName = groupName, sourceLangCode = sourceCode, targetLangCode = targetCode)
  }

  private fun currentConcreteGroupName(groupName: String? = selectedGroupName.value): String? {
    return groupName?.takeUnless { AggregateLanguageGroup.isAllGroup(it) }
  }

  private fun activeEditScope(
    groupName: String? = currentConcreteGroupName(),
    sourceCode: String? = sourceLangCode.value,
    targetCode: String? = targetLangCode.value
  ): EditScope? {
    if (groupName == null || sourceCode == null || targetCode == null) return null
    return EditScope(groupName = groupName, sourceLangCode = sourceCode, targetLangCode = targetCode)
  }

  private fun syncActiveWorkspaceState(
    groupName: String? = currentConcreteGroupName(),
    sourceCode: String? = sourceLangCode.value,
    targetCode: String? = targetLangCode.value
  ) {
    val scope = activeEditScope(groupName, sourceCode, targetCode)
    val workspace = _scopedWorkspaceState.value
    _stagedChanges.value = workspace.stagedChanges(scope)
    _stagedDeletions.value = workspace.stagedDeletions(scope)
    _createdLanguages.value = workspace.createdLanguages(groupName)
  }

  private fun updateActiveStagedChanges(newChanges: Map<String, String>) {
    _stagedChanges.value = newChanges
    _scopedWorkspaceState.update { workspace ->
      workspace.withStagedChanges(activeEditScope(), newChanges)
    }
  }

  private fun updateActiveStagedDeletions(newDeletions: Set<String>) {
    _stagedDeletions.value = newDeletions
    _scopedWorkspaceState.update { workspace ->
      workspace.withStagedDeletions(activeEditScope(), newDeletions)
    }
  }

  private fun updateCreatedLanguagesForGroup(groupName: String?, languages: Set<String>) {
    _createdLanguages.value = languages
    _scopedWorkspaceState.update { workspace ->
      workspace.withCreatedLanguages(groupName, languages)
    }
  }

  private fun scopedChangesFor(groupName: String, targetLangCode: String): Map<String, String> {
    val merged = linkedMapOf<String, String>()
    _scopedWorkspaceState.value.stagedChangesByScope.forEach { (scope, staged) ->
      if (scope.groupName == groupName && scope.targetLangCode == targetLangCode) {
        merged.putAll(staged)
      }
    }
    return merged
  }

  private fun scopedDeletionsFor(groupName: String, targetLangCode: String): Set<String> {
    val deletions = linkedSetOf<String>()
    _scopedWorkspaceState.value.stagedDeletionsByScope.forEach { (scope, staged) ->
      if (scope.groupName == groupName && scope.targetLangCode == targetLangCode) {
        deletions += staged
      }
    }
    return deletions
  }

  private fun effectiveTargetProperties(group: LanguageGroup, targetLangCode: String): Properties {
    val targetProps = group.languages[targetLangCode]?.properties ?: Properties()
    val mergedTarget = Properties()
    mergedTarget.putAll(targetProps)
    scopedChangesFor(group.name, targetLangCode).forEach { (key, value) ->
      if (value.isNotBlank()) {
        mergedTarget.setProperty(key, value)
      }
    }
    scopedDeletionsFor(group.name, targetLangCode).forEach { key ->
      mergedTarget.remove(key)
    }
    return mergedTarget
  }

  private fun selectedGroupsFor(sourceCode: String, targetCode: String): List<LanguageGroup> {
    return if (AggregateLanguageGroup.isAllGroup(selectedGroupName.value)) {
      _languageGroups.value.filter { group ->
        group.languages.containsKey(sourceCode) && group.languages.containsKey(targetCode)
      }
    } else {
      _languageGroups.value.filter { it.name == selectedGroupName.value }
    }
  }

  private fun applyDictionaryToGroup(
    group: LanguageGroup,
    sourceCode: String,
    targetCode: String
  ): Map<String, String> {
    val sourceProps = group.languages[sourceCode]?.properties ?: return emptyMap()
    val targetProps = group.languages[targetCode]?.properties ?: Properties()
    val stagedChanges = scopedChangesFor(group.name, targetCode)
    val stagedDeletions = scopedDeletionsFor(group.name, targetCode)

    val entries = ((sourceProps.keys + targetProps.keys).mapNotNull { it as? String } + stagedChanges.keys)
      .distinct()
      .sorted()
      .mapNotNull { key ->
        if (stagedDeletions.contains(key)) {
          return@mapNotNull null
        }
        val sourceValue = sourceProps.getProperty(key, "")
        val originalTargetValue = if (targetProps.containsKey(key)) targetProps.getProperty(key, "") else ""
        TranslationEntry(
          key = key,
          sourceValue = sourceValue,
          targetValue = stagedChanges[key] ?: originalTargetValue,
          originalTargetValue = originalTargetValue,
          isUntranslated = false,
          isMissing = !targetProps.containsKey(key) && sourceProps.containsKey(key),
          isDeleted = targetProps.containsKey(key) && !sourceProps.containsKey(key)
        )
      }

    return dictionaryManager.applyToEntries(entries, group.name, sourceCode, targetCode)
  }

  private suspend fun recordTranslationMemoryForWorkspaceChanges() {
    val groupsByName = _languageGroups.value.associateBy { it.name }
    for ((scope, staged) in _scopedWorkspaceState.value.stagedChangesByScope) {
      if (staged.isEmpty()) continue
      val group = groupsByName[scope.groupName] ?: continue
      val sourceProps = group.languages[scope.sourceLangCode]?.properties ?: continue
      val targetProps = group.languages[scope.targetLangCode]?.properties ?: continue
      val sourceMap = mutableMapOf<String, String>()
      val targetMap = mutableMapOf<String, String>()
      sourceProps.forEach { (key, value) -> sourceMap[key as String] = value as String }
      targetProps.forEach { (key, value) -> targetMap[key as String] = value as String }
      staged.forEach { (key, value) -> targetMap[key] = value }
      scopedDeletionsFor(scope.groupName, scope.targetLangCode).forEach { key -> targetMap.remove(key) }
      translationMemory.importFromEntries(sourceMap, targetMap, scope.sourceLangCode, scope.targetLangCode)
    }
    translationMemory.save()
  }

  private fun clearEntryState() {
    _allEntries.value = emptyList()
    _missingEntriesCount.value = 0
    _deletedEntriesCount.value = 0
    _diffEntriesCount.value = 0
    _deletedItems.value = emptyList()
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

  fun setSearchQuery(query: String) {
    searchQuery.value = query
    currentPage.value = 1
    _currentSearchResultIndex.value = if (query.isBlank()) -1 else 0
  }
  fun setReplaceQuery(query: String) { replaceQuery.value = query }
  fun setCaseSensitive(isSensitive: Boolean) {
    isCaseSensitive.value = isSensitive
    currentPage.value = 1
    if (searchQuery.value.isNotBlank()) _currentSearchResultIndex.value = 0
  }
  fun setExactMatch(isExact: Boolean) {
    isExactMatch.value = isExact
    currentPage.value = 1
    if (searchQuery.value.isNotBlank()) _currentSearchResultIndex.value = 0
  }
  fun setFilter(filter: FilterState) {
    filterState.value = filter
    currentPage.value = 1
    _currentSearchResultIndex.value = if (searchQuery.value.isBlank()) -1 else 0
    regenerateEntries()
  }
  fun nextPage() { if (currentPage.value < totalPages.value) currentPage.value++ }
  fun previousPage() { if (currentPage.value > 1) currentPage.value-- }
  fun setShowAboutDialog(show: Boolean) { _showAboutDialog.value = show }
  fun setThemeColor(theme: ThemeColor) {
    _themeColor.value = theme
    prefs.edit().putString("theme_color", theme.name).apply()
  }
  fun toggleSearchCardVisibility() { _isSearchCardVisible.value = !_isSearchCardVisible.value }

  fun focusFirstSearchResult() {
    setCurrentSearchResultIndex(0)
  }

  fun nextSearchResult() {
    val count = searchResultCount.value
    if (count <= 0) return
    val current = _currentSearchResultIndex.value.takeIf { it >= 0 } ?: 0
    setCurrentSearchResultIndex((current + 1) % count)
  }

  fun previousSearchResult() {
    val count = searchResultCount.value
    if (count <= 0) return
    val current = _currentSearchResultIndex.value.takeIf { it >= 0 } ?: 0
    setCurrentSearchResultIndex((current - 1 + count) % count)
  }

  fun replaceCurrentMatch(): Int {
    val key = currentSearchResultKey.value ?: return 0
    val search = searchQuery.value
    val replace = replaceQuery.value
    if (search.isBlank()) return 0
    val entry = _allEntries.value.find { it.key == key } ?: return 0
    val currentValue = _stagedChanges.value[key] ?: entry.targetValue
    val replaced = SearchReplaceUtils.replaceTarget(
      original = currentValue,
      search = search,
      replacement = replace,
      caseSensitive = isCaseSensitive.value,
      exactMatch = isExactMatch.value
    )
    if (replaced == currentValue) return 0
    stageChange(key, replaced)
    return 1
  }

  /** Replace all occurrences of searchQuery in target values of displayed entries with replaceQuery */
  fun replaceAllMatching(): Int {
    val search = searchQuery.value
    val replace = replaceQuery.value
    if (search.isBlank()) return 0
    val caseSensitive = isCaseSensitive.value
    val exact = isExactMatch.value
    val matchKeys = _searchResultKeys.value
    if (matchKeys.isEmpty()) return 0
    val entriesByKey = _allEntries.value.associateBy { it.key }
    var count = 0

    val newStaged = _stagedChanges.value.toMutableMap()
    for (key in matchKeys) {
      val entry = entriesByKey[key] ?: continue
      if (entry.isDeleted) continue
      val current = newStaged[entry.key] ?: entry.targetValue
      if (current.isBlank()) continue

      val replaced = SearchReplaceUtils.replaceTarget(
        original = current,
        search = search,
        replacement = replace,
        caseSensitive = caseSensitive,
        exactMatch = exact
      )

      if (replaced != current) {
        newStaged[entry.key] = replaced
        count++
      }
    }
    updateActiveStagedChanges(newStaged)

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
          successCount += result.success
          ignoreCount += result.ignored
          overwriteCount += result.overwritten
          failCount += result.failed
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
      } else if (uris.isNotEmpty()) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.import_empty)))
      }

      checkDraftAfterLoad()
    }
  }

  private enum class LoadResult { SUCCESS, OVERWRITE, FAIL, IGNORE }

  private data class ZipLoadResult(
    val success: Int,
    val ignored: Int,
    val overwritten: Int,
    val failed: Int
  )

  private fun loadFromZip(
    resolver: ContentResolver,
    uri: Uri,
    groups: MutableMap<String, MutableMap<String, LanguageData>>
  ): ZipLoadResult {
    var success = 0
    var ignored = 0
    var overwritten = 0
    var failed = 0
    val inputStream = resolver.openInputStream(uri)
      ?: return ZipLoadResult(success = 0, ignored = 0, overwritten = 0, failed = 1)
    inputStream.use { zipInputStream ->
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
                } catch (e: Exception) {
                  Log.w("TranslatorVM", "Failed to parse ZIP entry: $entryName", e)
                  failed++
                }
              }
            }
          }
        }
      }
    }
    return ZipLoadResult(success = success, ignored = ignored, overwritten = overwritten, failed = failed)
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
    val nextAvailableLanguages = AggregateLanguageGroup.availableLanguages(_languageGroups.value, name)
    val preservedSource = sourceLangCode.value?.takeIf { it in nextAvailableLanguages && it != targetLangCode.value }
    val preservedTarget = targetLangCode.value?.takeIf { it in nextAvailableLanguages && it != preservedSource }
    sourceLangCode.value = preservedSource
    targetLangCode.value = preservedTarget
    syncActiveWorkspaceState(
      groupName = currentConcreteGroupName(name),
      sourceCode = preservedSource,
      targetCode = preservedTarget
    )
    clearEntryState()
    resetSearchState()
    availableLanguages.value = nextAvailableLanguages
    if (preservedSource != null && preservedTarget != null) {
      regenerateEntries()
    }
  }

  fun selectSourceLanguage(code: String) {
    if (code == targetLangCode.value) return
    sourceLangCode.value = code
    syncActiveWorkspaceState(sourceCode = code)
    if (targetLangCode.value != null) regenerateEntries() else clearEntryState()
  }

  fun selectTargetLanguage(code: String) {
    if (code == sourceLangCode.value) return
    targetLangCode.value = code
    syncActiveWorkspaceState(targetCode = code)
    if (sourceLangCode.value != null) regenerateEntries() else clearEntryState()
  }

  fun stageChange(key: String, newTargetValue: String) {
    val newStaged = _stagedChanges.value.toMutableMap()
    newStaged[key] = newTargetValue
    updateActiveStagedChanges(newStaged)
  }

  fun unstageChange(key: String) {
    val newStaged = _stagedChanges.value.toMutableMap()
    newStaged.remove(key)
    updateActiveStagedChanges(newStaged)
  }

  fun selectDictionary(id: String) {
    dictionarySelectionRunner.launch {
      selectDictionaryPersisted(
        id = id,
        persistSelection = { selectedId ->
          dictionaryManager.selectDictionary(selectedId)
          dictionaryManager.save()
        },
        onSelectionApplied = {
          refreshDictionaryState()
          regenerateEntries()
        }
      )
    }
  }

  fun createDictionary(name: String) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        dictionaryManager.createDictionary(name)
        dictionaryManager.save()
        refreshDictionaryState()
        regenerateEntries()
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_create_done, dictionaryManager.getSelectedDictionaryName())))
      } catch (e: IllegalArgumentException) {
        val messageId = if (e.message == "duplicate_name") R.string.dict_name_exists else R.string.dict_name_invalid
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(messageId)))
      }
    }
  }

  fun renameCurrentDictionary(name: String) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        dictionaryManager.renameSelectedDictionary(name)
        dictionaryManager.save()
        refreshDictionaryState()
        regenerateEntries()
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_rename_done, dictionaryManager.getSelectedDictionaryName())))
      } catch (e: IllegalArgumentException) {
        val messageId = if (e.message == "duplicate_name") R.string.dict_name_exists else R.string.dict_name_invalid
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(messageId)))
      }
    }
  }

  fun deleteCurrentDictionary() {
    viewModelScope.launch(Dispatchers.IO) {
      val deleted = dictionaryManager.deleteSelectedDictionary()
      if (!deleted) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_delete_last_blocked)))
        return@launch
      }
      dictionaryManager.save()
      refreshDictionaryState()
      regenerateEntries()
      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_delete_done)))
    }
  }

  // --- Deletion Functions ---

  fun stageDeleteEntry(key: String) {
    updateActiveStagedDeletions(_stagedDeletions.value + key)
    // A3: Update deletedItems to reflect staged status
    _deletedItems.update { items ->
      items.map { if (it.key == key) it.copy(isStagedForDeletion = true) else it }
    }
  }

  // A3: Unstage a deletion
  fun unstageDeleteEntry(key: String) {
    updateActiveStagedDeletions(_stagedDeletions.value - key)
    _deletedItems.update { items ->
      items.map { if (it.key == key) it.copy(isStagedForDeletion = false) else it }
    }
  }

  fun deleteAllDeletedEntries() {
    viewModelScope.launch {
      val items = _deletedItems.value
      val unstagedKeys = items.filter { !it.isStagedForDeletion }.map { it.key }.toSet()
      updateActiveStagedDeletions(_stagedDeletions.value + unstagedKeys)
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
      val groups = selectedGroupsFor(sourceCode, targetCode)
      if (groups.isEmpty()) return@launch

      var count = 0
      groups.forEach { group ->
        val sourceProps = group.languages[sourceCode]?.properties ?: return@forEach
        val mergedTarget = effectiveTargetProperties(group, targetCode)
        count += dictionaryManager.importFromProperties(sourceProps, mergedTarget, group.name, sourceCode, targetCode)
      }
      dictionaryManager.save()
      refreshDictionaryState()

      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_save_done, count)))
    }
  }

  fun applyDictionary() {
    viewModelScope.launch(Dispatchers.Default) {
      val sourceCode = sourceLangCode.value ?: return@launch
      val targetCode = targetLangCode.value ?: return@launch
      val groups = selectedGroupsFor(sourceCode, targetCode)
      if (groups.isEmpty()) return@launch

      var appliedCount = 0
      groups.forEach { group ->
        val applied = applyDictionaryToGroup(group, sourceCode, targetCode)
        if (applied.isEmpty()) return@forEach

        val scope = EditScope(group.name, sourceCode, targetCode)
        val merged = linkedMapOf<String, String>()
        merged.putAll(_scopedWorkspaceState.value.stagedChanges(scope))
        applied.forEach { (key, value) -> merged[key] = value }
        _scopedWorkspaceState.update { workspace ->
          workspace.withStagedChanges(scope, merged)
        }
        appliedCount += applied.size
      }

      if (appliedCount == 0) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_apply_none)))
        return@launch
      }

      syncActiveWorkspaceState()
      regenerateEntries()

      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_apply_done, appliedCount)))
    }
  }

  fun clearDictionary() {
    viewModelScope.launch {
      dictionaryManager.clear()
      refreshDictionaryState()
      regenerateEntries()
      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_clear_done)))
    }
  }

  fun showDictionaryPreview() {
    dictionaryPreviewQuery.value = ""
    dictionaryPreviewEntries.value = dictionaryManager.getPreviewEntries("")
    isDictionaryPreviewVisible.value = true
  }

  fun hideDictionaryPreview() {
    isDictionaryPreviewVisible.value = false
    dictionaryPreviewQuery.value = ""
  }

  fun setDictionaryPreviewQuery(query: String) {
    dictionaryPreviewQuery.value = query
    dictionaryPreviewEntries.value = dictionaryManager.getPreviewEntries(query)
  }

  fun updateDictionaryPreviewEntry(rawKey: String, sourceText: String, translation: String) {
    if (translation.isBlank()) return
    viewModelScope.launch(Dispatchers.IO) {
      dictionaryManager.updatePreviewEntry(rawKey, sourceText, translation)
      dictionaryManager.save()
      refreshDictionaryState()
      regenerateEntries()
      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_preview_save_done)))
    }
  }

  fun currentDictionaryExportFileName(): String = dictionaryManager.currentDictionaryExportFileName()

  fun allDictionariesExportFileName(): String = dictionaryManager.allDictionariesExportFileName()

  fun exportCurrentDictionary(resolver: ContentResolver, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.export_fail_no_stream)))
          return@launch
        }
        outputStream.use { stream ->
          stream.write(dictionaryManager.exportSelectedDictionary())
        }
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_export_current_done)))
      } catch (_: Exception) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_export_fail)))
      }
    }
  }

  fun exportAllDictionaries(resolver: ContentResolver, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.export_fail_no_stream)))
          return@launch
        }
        outputStream.use { stream ->
          stream.write(dictionaryManager.exportAllDictionaries())
        }
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_export_all_done)))
      } catch (_: Exception) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_export_fail)))
      }
    }
  }

  fun importDictionaryFromUri(resolver: ContentResolver, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      pendingDictionaryImport.value = null
      val fileName = getFileName(resolver, uri) ?: "dictionary.pddict.json"
      try {
        val inputStream = resolver.openInputStream(uri)
        if (inputStream == null) {
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_import_fail)))
          return@launch
        }
        val bytes = inputStream.use { it.readBytes() }
        val preview = dictionaryManager.previewImportDictionaryPayload(fileName, bytes)
        if (preview.conflictNames.isNotEmpty()) {
          pendingDictionaryImport.value = PendingDictionaryImport(
            fileName = fileName,
            bytes = bytes,
            importedCount = preview.importedCount,
            conflictNames = preview.conflictNames
          )
          return@launch
        }
        applyDictionaryImport(fileName, bytes)
      } catch (e: IllegalArgumentException) {
        val messageId = if (e.message == "missing_index" || e.message == "invalid_index" || e.message == "missing_dictionaries" || e.message == "invalid_dictionary") {
          R.string.dict_import_invalid
        } else {
          R.string.dict_import_fail
        }
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(messageId)))
      } catch (_: Exception) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_import_fail)))
      }
    }
  }

  fun confirmRenameAllDictionaryConflicts() {
    val pending = pendingDictionaryImport.value ?: return
    viewModelScope.launch(Dispatchers.IO) {
      try {
        applyDictionaryImport(pending.fileName, pending.bytes)
      } catch (_: Exception) {
        _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_import_fail)))
      } finally {
        pendingDictionaryImport.value = null
      }
    }
  }

  fun cancelPendingDictionaryImport() {
    pendingDictionaryImport.value = null
  }

  // --- Custom Language Creation ---

  // A4: Language code validation + normalization
  fun createLanguage(langCode: String, copyFromLang: String? = null) {
    viewModelScope.launch {
      val groupName = currentConcreteGroupName()
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
      availableLanguages.value = AggregateLanguageGroup.availableLanguages(_languageGroups.value, selectedGroupName.value)
      updateCreatedLanguagesForGroup(groupName, _createdLanguages.value + normalizedCode)

      if (sourceLangCode.value != normalizedCode) {
        selectTargetLanguage(normalizedCode)
      }

      _createLanguageSuccessSerial.value += 1
      _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.create_lang_done, normalizedCode)))
    }
  }

  // --- Save (optimized: only modified files) ---

  // A2: Export with proper error handling
  fun saveChangesToZip(resolver: ContentResolver, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        _autoSaveEnabled = false

        val groups = _languageGroups.value
        if (groups.isEmpty() || !hasPendingWorkspaceChanges()) {
          return@launch
        }

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
              groups.forEach { group ->
                val createdLangs = _scopedWorkspaceState.value.createdLanguages(group.name)
                group.languages.forEach { (langCode, langData) ->
                  val effectiveStaged = scopedChangesFor(group.name, langCode).filter { (key, value) ->
                    val originalValue = langData.properties.getProperty(key, "")
                    val existsInTarget = langData.properties.containsKey(key)
                    value != originalValue || !existsInTarget
                  }
                  val deletions = scopedDeletionsFor(group.name, langCode)
                  val isNewlyCreated = langCode in createdLangs

                  if (effectiveStaged.isEmpty() && deletions.isEmpty() && !isNewlyCreated) {
                    return@forEach
                  }

                  val finalProps = Properties()
                  finalProps.putAll(langData.properties)
                  effectiveStaged.forEach { (key, value) ->
                    finalProps.setProperty(key, value)
                  }
                  deletions.forEach { key -> finalProps.remove(key) }

                  zos.putNextEntry(ZipEntry(langData.fileName))
                  val writer = OutputStreamWriter(zos, "UTF-8")
                  PropertiesWriter.write(finalProps, writer)
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
          recordTranslationMemoryForWorkspaceChanges()
        } catch (e: Exception) {
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.tm_record_fail)))
        }

        // Delete draft (only after successful write)
        draftManager.delete()
        _draftData.value = null
        _draftValidation.value = DraftValidation.NO_DRAFT

        _scopedWorkspaceState.value = ScopedWorkspaceState()
        syncActiveWorkspaceState()
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
    val workspace = _scopedWorkspaceState.value
    if (!hasPendingWorkspaceChanges(workspace, _languageGroups.value)) return

    val scope = primaryDraftScope(workspace) ?: return
    val groupName = scope.groupName
    val srcLang = scope.sourceLangCode
    val tgtLang = scope.targetLangCode
    val staged = workspace.stagedChanges(scope)
    val deletions = workspace.stagedDeletions(scope)
    val createdLangs = workspace.createdLanguages(groupName)
    val group = _languageGroups.value.find { it.name == groupName } ?: return
    val srcProps = group.languages[srcLang]?.properties ?: return
    val tgtProps = group.languages[tgtLang]?.properties ?: Properties()
    val keys = (srcProps.keys + tgtProps.keys).mapNotNull { it as? String }.distinct().sorted()
    val digest = DraftManager.computeKeysDigest(keys)

    // A5: Compute contentDigest (includes source text)
    val contentDigest = DraftManager.computeContentDigest(keys.map { key -> key to srcProps.getProperty(key, "") })

    // Serialize created language properties for persistence
    val createdLangData: Map<String, Map<String, String>>? = if (createdLangs.isNotEmpty()) {
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
      entryCount = keys.size,
      keysDigest = digest,
      timestamp = System.currentTimeMillis(),
      stagedDeletions = deletions,
      createdLanguages = createdLangData,
      contentDigest = contentDigest,
      workspaceSnapshot = DraftWorkspaceSnapshot.capture(workspace, _languageGroups.value)
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
      val restorePlan = DraftRestorePlan.fromDraft(draft)
      val snapshot = DraftWorkspaceSnapshot.fromDraft(draft)
      _languageGroups.update { groups ->
        groups.map { group ->
          val createdLanguages = snapshot.createdLanguages(group.name)
          if (createdLanguages.isEmpty()) {
            group
          } else {
            val newLanguages = group.languages.toMutableMap()
            createdLanguages.forEach { (langCode, propsMap) ->
              if (!newLanguages.containsKey(langCode)) {
                val newFileName = "${group.name}_${langCode}.properties"
                val props = Properties()
                propsMap.forEach { (key, value) -> props.setProperty(key, value) }
                newLanguages[langCode] = LanguageData(newFileName, props)
              }
            }
            group.copy(languages = newLanguages)
          }
        }
      }
      _scopedWorkspaceState.value = snapshot.toScopedWorkspaceState()

      sourceLangCode.value = restorePlan.preselectedSourceLangCode
      targetLangCode.value = restorePlan.preselectedTargetLangCode
      selectGroup(restorePlan.groupName)
      selectSourceLanguage(restorePlan.finalSourceLangCode)
      selectTargetLanguage(restorePlan.finalTargetLangCode)
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
        if (result.isSuccess) {
          _networkSuggestion.value = NetworkSuggestionState(
            entryKey = key,
            requestId = thisRequestId,
            results = listOf(result.getOrThrow())
          )
        } else {
          _networkSuggestion.value = null
          val friendly = engineManager.getFriendlyError(engineManager.getSelectedEngineId(), result.exceptionOrNull())
          _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.engine_translate_fail, friendly)))
        }
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
      _scopedWorkspaceState.value = ScopedWorkspaceState()

      val groupList = _languageGroups.value
      if (groupList.size == 1) {
        val group = groupList.first()
        selectedGroupName.value = group.name
        availableLanguages.value = AggregateLanguageGroup.availableLanguages(_languageGroups.value, group.name)

        val langs = availableLanguages.value
        if (langs.size == 2) {
          val sourceIdx = langs.indexOfFirst { it == "base" }.takeIf { it >= 0 } ?: 0
          val targetIdx = if (sourceIdx == 0) 1 else 0
          sourceLangCode.value = langs[sourceIdx]
          targetLangCode.value = langs[targetIdx]
          syncActiveWorkspaceState(group.name, langs[sourceIdx], langs[targetIdx])
          regenerateEntries()
        } else {
          sourceLangCode.value = null
          targetLangCode.value = null
          syncActiveWorkspaceState(groupName = group.name, sourceCode = null, targetCode = null)
          clearEntryState()
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
    clearEntryState()
    _stagedChanges.value = emptyMap()
    _stagedDeletions.value = emptySet()
    _createdLanguages.value = emptySet()
    _scopedWorkspaceState.value = ScopedWorkspaceState()
    resetSearchState()
  }

  private fun regenerateEntries() {
    val sourceCode = sourceLangCode.value
    val targetCode = targetLangCode.value
    val groupName = selectedGroupName.value
    val group = _languageGroups.value.find { it.name == groupName }

    if (sourceCode == null || targetCode == null || group == null || groupName == null || AggregateLanguageGroup.isAllGroup(groupName)) {
      clearEntryState()
      return
    }

    regenerateJob?.cancel()
    regenerateJob = viewModelScope.launch(Dispatchers.Default) {
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
        updateActiveStagedChanges(newStagedChanges)
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

  private fun refreshDictionaryState() {
    dictionaries.value = dictionaryManager.getDictionarySummaries()
    selectedDictionaryId.value = dictionaryManager.getSelectedDictionaryId()
    selectedDictionaryName.value = dictionaryManager.getSelectedDictionaryName()
    dictionaryCount.value = dictionaryManager.getDictionaryCount()
    canDeleteDictionary.value = dictionaryManager.canDeleteSelectedDictionary()
    _dictEntryCount.value = dictionaryManager.getTotalCount()
    dictionaryPreviewEntries.value = dictionaryManager.getPreviewEntries(dictionaryPreviewQuery.value)
  }

  private suspend fun applyDictionaryImport(fileName: String, bytes: ByteArray) {
    val result = dictionaryManager.importDictionaryPayload(fileName, bytes)
    pendingDictionaryImport.value = null
    dictionaryManager.save()
    refreshDictionaryState()
    regenerateEntries()
    _uiEvents.send(UiEvent.ShowSnackbar(app.getString(R.string.dict_import_done, result.importedCount)))
  }

  private fun setCurrentSearchResultIndex(index: Int) {
    val count = searchResultCount.value
    if (count <= 0) {
      resetSearchState()
      return
    }
    val normalized = index.coerceIn(0, count - 1)
    _currentSearchResultIndex.value = normalized
    currentSearchResultKey.value = _searchResultKeys.value.getOrNull(normalized)
    currentPage.value = normalized / pageSize + 1
  }

  private fun resetSearchState() {
    searchQuery.value = ""
    replaceQuery.value = ""
    _searchResultKeys.value = emptyList()
    searchResultCount.value = 0
    _currentSearchResultIndex.value = -1
    currentSearchResultKey.value = null
    currentPage.value = 1
  }

  private fun loadSavedThemeColor(): ThemeColor {
    val raw = prefs.getString("theme_color", ThemeColor.PIXEL_DUNGEON.name) ?: ThemeColor.PIXEL_DUNGEON.name
    return runCatching { ThemeColor.valueOf(raw) }.getOrDefault(ThemeColor.PIXEL_DUNGEON)
  }

  fun getLanguageDisplayName(code: String, context: Context): String {
    return LanguageUtils.getDisplayName(code, context)
  }

  override fun onCleared() {
    dictionarySelectionRunner.cancel()
    networkQueryJob?.cancel()
    tmQueryJob?.cancel()
    _uiEvents.close()
    super.onCleared()
  }
}
