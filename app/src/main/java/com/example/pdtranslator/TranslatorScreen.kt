package com.example.pdtranslator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranslatorScreen(viewModel: TranslatorViewModel) {
  val displayEntries by viewModel.displayEntries.collectAsState()
  val filterState by viewModel.filterState.collectAsState()
  val currentPage by viewModel.currentPage.collectAsState()
  val totalPages by viewModel.totalPages.collectAsState()
  val infoBarText by viewModel.infoBarText.collectAsState()
  val translationProgress by viewModel.translationProgress.collectAsState()
  val isSearchCardVisible by viewModel.isSearchCardVisible.collectAsState()
  val missingEntriesCount by viewModel.missingEntriesCount.collectAsState()
  val highlightKeywords by viewModel.highlightKeywords.collectAsState()
  val tmSuggestions by viewModel.tmSuggestions.collectAsState()
  val networkSuggestion by viewModel.networkSuggestion.collectAsState()
  val deletedItems by viewModel.deletedItems.collectAsState()
  val languageGroupNames by viewModel.languageGroupNames.collectAsState()
  val selectedGroupName by viewModel.selectedGroupName.collectAsState()
  val sourceLangCode by viewModel.sourceLangCode.collectAsState()
  val targetLangCode by viewModel.targetLangCode.collectAsState()
  val availableLanguages by viewModel.availableLanguages.collectAsState()
  val currentSearchResultKey by viewModel.currentSearchResultKey.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val context = LocalContext.current
  val searchListState = rememberLazyListState()
  val usesPrefixGrouping = filterState == FilterState.DIFF || filterState == FilterState.ALL ||
    filterState == FilterState.UNTRANSLATED || filterState == FilterState.TRANSLATED ||
    filterState == FilterState.MODIFIED || filterState == FilterState.NO_TRANSLATION_NEEDED
  val groupedEntries = remember(displayEntries) { groupEntriesByPrefix(displayEntries) }
  var collapsedGroups by remember(filterState) { mutableStateOf(emptySet<String>()) }

  LaunchedEffect(displayEntries, currentSearchResultKey, usesPrefixGrouping) {
    val effectiveCollapsedGroups = if (usesPrefixGrouping) {
      revealCurrentSearchResultGroup(
        collapsedGroups = collapsedGroups,
        entries = displayEntries,
        currentSearchResultKey = currentSearchResultKey
      )
    } else {
      collapsedGroups
    }

    if (usesPrefixGrouping) {
      collapsedGroups = effectiveCollapsedGroups
    }

    val targetIndex = if (usesPrefixGrouping) {
      findGroupedSearchResultScrollIndex(
        entries = displayEntries,
        currentSearchResultKey = currentSearchResultKey,
        collapsedGroups = effectiveCollapsedGroups
      )
    } else {
      findSearchResultScrollIndex(displayEntries, currentSearchResultKey)
    } ?: return@LaunchedEffect
    searchListState.animateScrollToItem(targetIndex)
  }

  // Empty state: no files loaded
  if (languageGroupNames.isEmpty()) {
    EmptyState(
      icon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
      message = stringResource(R.string.empty_no_files)
    )
    return
  }

  // Languages not fully selected — show inline selectors
  if (selectedGroupName == null || sourceLangCode == null || targetLangCode == null) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = stringResource(R.string.empty_select_languages),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      LanguageGroupSelector(languageGroupNames, selectedGroupName, viewModel::selectGroup)
      LanguageSelectors(
        availableLanguages = availableLanguages,
        sourceLangCode = sourceLangCode,
        targetLangCode = targetLangCode,
        onSourceSelected = viewModel::selectSourceLanguage,
        onTargetSelected = viewModel::selectTargetLanguage,
        getDisplayName = { code -> viewModel.getLanguageDisplayName(code, context) }
      )
    }
    return
  }

  if (AggregateLanguageGroup.isAllGroup(selectedGroupName)) {
    EmptyState(
      icon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
      message = stringResource(R.string.translator_all_group_readonly)
    )
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Spacer(modifier = Modifier.height(4.dp))

    AnimatedVisibility(visible = isSearchCardVisible) {
      SearchReplaceControls(viewModel)
    }

    // Progress bar + percentage info + toggle
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = infoBarText,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { viewModel.toggleSearchCardVisibility() }, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = if (isSearchCardVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isSearchCardVisible) "Collapse Search" else "Expand Search",
            modifier = Modifier.size(20.dp)
          )
        }
      }
      LinearProgressIndicator(
        progress = translationProgress,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )
    }

    FilterButtons(filterState) { viewModel.setFilter(it) }

    when {
      filterState == FilterState.MISSING -> {
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Button(
            onClick = { viewModel.fillMissingEntries() },
            enabled = missingEntriesCount > 0
          ) {
            Text(stringResource(id = R.string.translator_complete_missing))
          }
        }
      }
      filterState == FilterState.DELETED -> {
        // A3: Use deletedItems (includes both staged and unstaged)
        val unstagedCount = deletedItems.count { !it.isStagedForDeletion }
        if (deletedItems.isNotEmpty() && unstagedCount > 0) {
          Button(
            onClick = { viewModel.deleteAllDeletedEntries() },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.deleted_delete_all, unstagedCount))
          }
        }
        if (deletedItems.isEmpty()) {
          EmptyState(
            icon = null,
            message = stringResource(R.string.deleted_empty)
          )
        } else {
          // A3: Own pagination for deletedItems
          val deletedPageSize = 20
          val deletedTotalPages = ((deletedItems.size + deletedPageSize - 1) / deletedPageSize).coerceAtLeast(1)
          val deletedPage = currentPage.coerceIn(1, deletedTotalPages)
          val pagedDeletedItems = deletedItems.chunked(deletedPageSize).getOrElse(deletedPage - 1) { emptyList() }

          LazyColumn(
            modifier = Modifier.weight(1f),
            state = searchListState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(pagedDeletedItems, key = { it.key }) { item ->
              if (item.isStagedForDeletion) {
                StagedDeletedCard(
                  item = item,
                  onUndo = { viewModel.unstageDeleteEntry(item.key) }
                )
              } else {
                DeletedTranslationCard(
                  entry = TranslationEntry(
                    key = item.key,
                    sourceValue = "",
                    targetValue = item.targetValue,
                    originalTargetValue = item.targetValue,
                    isUntranslated = false,
                    isDeleted = true
                  ),
                  onDelete = { viewModel.stageDeleteEntry(item.key) }
                )
              }
            }
          }
          PaginationControls(deletedPage, deletedTotalPages, viewModel::previousPage, viewModel::nextPage, viewModel::goToPage)
        }
      }
      filterState == FilterState.DIFF -> {
        if (displayEntries.isEmpty()) {
          EmptyState(
            icon = null,
            message = stringResource(R.string.diff_empty)
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            state = searchListState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            groupedEntries.forEach { (prefix, entries) ->
              stickyHeader(key = "diff_header_$prefix") {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                      collapsedGroups = if (prefix in collapsedGroups) {
                        collapsedGroups - prefix
                      } else {
                        collapsedGroups + prefix
                      }
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    if (prefix in collapsedGroups) Icons.AutoMirrored.Filled.KeyboardArrowRight
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = stringResource(R.string.group_header_count, prefix, entries.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
              if (prefix !in collapsedGroups) {
                items(entries, key = { it.key }) { entry ->
                  DiffTranslationCard(
                    entry = entry,
                    highlightKeywords = highlightKeywords,
                    searchHighlightQuery = searchQuery,
                    isCurrentSearchResult = currentSearchResultKey == entry.key,
                    onSave = { newText -> viewModel.stageChange(entry.key, newText) },
                    onRevertToDict = {
                      entry.dictValue?.let { viewModel.stageChange(entry.key, it) }
                    },
                    onCalibrate = { key, original, calibrated -> viewModel.calibrateSource(key, original, calibrated) }
                  )
                }
              }
            }
          }
          PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage, viewModel::goToPage)
        }
      }
      displayEntries.isEmpty() -> {
        EmptyState(
          icon = null,
          message = stringResource(R.string.empty_no_entries)
        )
      }
      else -> {
        LazyColumn(
          modifier = Modifier.weight(1f),
          state = searchListState,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          groupedEntries.forEach { (prefix, entries) ->
            stickyHeader(key = "header_$prefix") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surface)
                  .clickable {
                    collapsedGroups = if (prefix in collapsedGroups) {
                      collapsedGroups - prefix
                    } else {
                      collapsedGroups + prefix
                    }
                  }
                  .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  if (prefix in collapsedGroups) Icons.AutoMirrored.Filled.KeyboardArrowRight
                  else Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.group_header_count, prefix, entries.size),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
            if (prefix !in collapsedGroups) {
              items(entries, key = { it.key }) { entry ->
                NewTranslationCard(
                  entry = entry,
                  highlightKeywords = highlightKeywords,
                  searchHighlightQuery = searchQuery,
                  isCurrentSearchResult = currentSearchResultKey == entry.key,
                  tmSuggestions = tmSuggestions,
                  networkSuggestion = networkSuggestion,
                  onSave = { newText -> viewModel.stageChange(entry.key, newText) },
                  onDiscard = { viewModel.unstageChange(entry.key) },
                  onFocused = { viewModel.requestTmSuggestions(entry.sourceValue) },
                  onUnfocused = { viewModel.clearTmSuggestions() },
                  onApplyTm = { targetText -> viewModel.applyTmSuggestion(entry.key, targetText) },
                  onTranslate = { viewModel.translateEntry(entry.key, entry.sourceValue) },
                  onApplyNetwork = { text -> viewModel.applyNetworkSuggestion(entry.key, text) },
                  hasEngine = viewModel.engineManager.getSelectedEngineId().isNotBlank(),
                  onCalibrate = { key, original, calibrated -> viewModel.calibrateSource(key, original, calibrated) },
                  isNoTranslationNeeded = entry.isNoTranslationNeeded,
                  onMarkNoTranslation = { viewModel.markNoTranslationNeeded(entry.key, entry.sourceValue) },
                  onUnmarkNoTranslation = { viewModel.unmarkNoTranslationNeeded(entry.key) }
                )
              }
            }
          }
        }
        PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage, viewModel::goToPage)
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
  }
}

@Composable
private fun EmptyState(icon: (@Composable () -> Unit)?, message: String) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (icon != null) {
      icon()
      Spacer(Modifier.height(16.dp))
    }
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchReplaceControls(viewModel: TranslatorViewModel) {
  val searchQuery by viewModel.searchQuery.collectAsState()
  val replaceQuery by viewModel.replaceQuery.collectAsState()
  val isCaseSensitive by viewModel.isCaseSensitive.collectAsState()
  val isExactMatch by viewModel.isExactMatch.collectAsState()
  val searchResultCount by viewModel.searchResultCount.collectAsState()
  val currentSearchResultIndex by viewModel.currentSearchResultIndex.collectAsState()
  val themeColor by viewModel.themeColor.collectAsState()
  val isPD = themeColor == ThemeColor.PIXEL_DUNGEON
  val context = LocalContext.current

  Card {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Search + Replace text fields
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.setSearchQuery(it) },
          label = { Text(stringResource(R.string.search_label)) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        OutlinedTextField(
          value = replaceQuery,
          onValueChange = { viewModel.setReplaceQuery(it) },
          label = { Text(stringResource(R.string.replace_label)) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
      }
      // Checkbox options
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          Modifier.selectable(
            selected = isCaseSensitive,
            onClick = { viewModel.setCaseSensitive(!isCaseSensitive) }),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(checked = isCaseSensitive, onCheckedChange = { viewModel.setCaseSensitive(it) })
          Text(stringResource(R.string.search_case_sensitive), style = MaterialTheme.typography.bodySmall)
        }
        Row(
          Modifier.selectable(
            selected = isExactMatch,
            onClick = { viewModel.setExactMatch(!isExactMatch) }),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(checked = isExactMatch, onCheckedChange = { viewModel.setExactMatch(it) })
          Text(stringResource(R.string.search_exact_match), style = MaterialTheme.typography.bodySmall)
        }
      }

      if (searchQuery.isNotBlank()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = stringResource(
              R.string.search_result_status,
              if (currentSearchResultIndex >= 0) currentSearchResultIndex + 1 else 0,
              searchResultCount
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { viewModel.previousSearchResult() },
              enabled = searchResultCount > 0
            ) {
              Text(stringResource(R.string.search_prev_btn), maxLines = 1)
            }
            OutlinedButton(
              onClick = { viewModel.nextSearchResult() },
              enabled = searchResultCount > 0
            ) {
              Text(stringResource(R.string.search_next_btn), maxLines = 1)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { viewModel.focusFirstSearchResult() },
          modifier = Modifier.weight(1f).height(36.dp),
          enabled = searchQuery.isNotBlank() && searchResultCount > 0,
          colors = if (isPD) ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.secondary
          ) else ButtonDefaults.outlinedButtonColors()
        ) {
          if (isPD) {
            Icon(painterResource(R.drawable.ic_pd_scroll), null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
          }
          Text(stringResource(R.string.search_find_btn), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
        Button(
          onClick = {
            val count = viewModel.replaceCurrentMatch()
            android.widget.Toast.makeText(
              context,
              context.getString(R.string.search_replace_current_done, count),
              android.widget.Toast.LENGTH_SHORT
            ).show()
          },
          modifier = Modifier.weight(1f).height(36.dp),
          enabled = searchQuery.isNotBlank() && replaceQuery.isNotEmpty() && searchResultCount > 0,
          colors = if (isPD) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
          ) else ButtonDefaults.buttonColors()
        ) {
          Text(stringResource(R.string.search_replace_current_btn), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
        Button(
          onClick = {
            val count = viewModel.replaceAllMatching()
            android.widget.Toast.makeText(
              context,
              context.getString(R.string.search_replace_done, count),
              android.widget.Toast.LENGTH_SHORT
            ).show()
          },
          modifier = Modifier.weight(1f).height(36.dp),
          enabled = searchQuery.isNotBlank() && replaceQuery.isNotEmpty(),
          colors = if (isPD) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
          ) else ButtonDefaults.buttonColors()
        ) {
          if (isPD) {
            Icon(painterResource(R.drawable.ic_pd_wand), null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
          }
          Text(stringResource(R.string.search_replace_btn), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTranslationCard(
  entry: TranslationEntry,
  highlightKeywords: Set<String>,
  searchHighlightQuery: String,
  isCurrentSearchResult: Boolean,
  tmSuggestions: List<TmSuggestion>,
  networkSuggestion: NetworkSuggestionState?,
  onSave: (String) -> Unit,
  onDiscard: () -> Unit,
  onFocused: () -> Unit,
  onUnfocused: () -> Unit,
  onApplyTm: (String) -> Unit,
  onTranslate: () -> Unit = {},
  onApplyNetwork: (String) -> Unit = {},
  hasEngine: Boolean = false,
  onCalibrate: (String, String, String) -> Unit = { _, _, _ -> },
  isNoTranslationNeeded: Boolean = false,
  onMarkNoTranslation: () -> Unit = {},
  onUnmarkNoTranslation: () -> Unit = {}
) {
  var currentText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }
  var lastCommittedText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }
  var isFocused by remember { mutableStateOf(false) }
  var isCalibrating by remember(entry.key) { mutableStateOf(false) }
  var calibratingText by remember(entry.key, entry.sourceValue) { mutableStateOf(entry.sourceValue) }
  val latestText = rememberUpdatedState(currentText)
  val latestCommittedText = rememberUpdatedState(lastCommittedText)
  val latestSave = rememberUpdatedState(onSave)
  val mergedHighlights = remember(highlightKeywords, searchHighlightQuery) {
    buildSet {
      addAll(highlightKeywords)
      if (searchHighlightQuery.isNotBlank()) add(searchHighlightQuery)
    }
  }
  val commitCurrentText = {
    if (currentText != lastCommittedText) {
      onSave(currentText)
      lastCommittedText = currentText
    }
  }

  DisposableEffect(entry.key) {
    onDispose {
      if (latestText.value != latestCommittedText.value) {
        latestSave.value(latestText.value)
      }
    }
  }

  val cardColors = if (isCurrentSearchResult) {
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
  } else if (entry.isModified) {
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
  } else {
    CardDefaults.cardColors()
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = cardColors,
    border = if (isCurrentSearchResult) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      // Key row with badges
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = entry.key,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        if (entry.isIdentical) {
          Spacer(Modifier.width(6.dp))
          BadgeLabel(
            text = stringResource(id = R.string.translator_identical_warning),
            color = MaterialTheme.colorScheme.error,
            background = MaterialTheme.colorScheme.errorContainer
          )
        }
        if (entry.isMissing) {
          Spacer(Modifier.width(6.dp))
          BadgeLabel(
            text = "Missing",
            color = MaterialTheme.colorScheme.error,
            background = MaterialTheme.colorScheme.errorContainer
          )
        }
        if (entry.isDiff) {
          Spacer(Modifier.width(6.dp))
          BadgeLabel(
            text = stringResource(R.string.diff_badge),
            color = MaterialTheme.colorScheme.tertiary,
            background = MaterialTheme.colorScheme.tertiaryContainer
          )
        }
        if (entry.isNoTranslationNeeded) {
          Spacer(Modifier.width(6.dp))
          BadgeLabel(
            text = stringResource(R.string.no_translation_needed_badge),
            color = MaterialTheme.colorScheme.secondary,
            background = MaterialTheme.colorScheme.secondaryContainer
          )
        }
      }

      // Source text with calibration support
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (entry.isCalibrated) {
            BadgeLabel(
              text = stringResource(R.string.calibration_badge),
              color = MaterialTheme.colorScheme.tertiary,
              background = MaterialTheme.colorScheme.tertiaryContainer
            )
            Spacer(Modifier.width(6.dp))
          }
          Spacer(Modifier.weight(1f))
          IconButton(
            onClick = {
              isCalibrating = !isCalibrating
              calibratingText = entry.sourceValue
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              Icons.Default.Edit,
              contentDescription = stringResource(R.string.calibration_btn),
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (isCalibrating) {
          OutlinedTextField(
            value = calibratingText,
            onValueChange = { calibratingText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.calibration_title)) },
            minLines = 2
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(onClick = { isCalibrating = false }) {
              Text(stringResource(R.string.common_cancel))
            }
            Spacer(Modifier.width(8.dp))
            Button(
              onClick = {
                onCalibrate(entry.key, entry.sourceValue, calibratingText)
                isCalibrating = false
              },
              enabled = calibratingText.isNotBlank() && calibratingText != entry.sourceValue
            ) {
              Text(stringResource(R.string.common_save))
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(horizontal = 10.dp, vertical = 8.dp)
          ) {
            HighlightedText(text = entry.sourceValue, keywords = mergedHighlights)
          }
        }
      }

      // Translation input
      OutlinedTextField(
        value = currentText,
        onValueChange = {
          currentText = it
        },
        modifier = Modifier
          .fillMaxWidth()
          .onFocusChanged { focusState ->
            if (focusState.isFocused && !isFocused) {
              isFocused = true
              onFocused()
            } else if (!focusState.isFocused && isFocused) {
              isFocused = false
              commitCurrentText()
              onUnfocused()
            }
          },
        label = { Text(stringResource(id = R.string.common_translation)) },
        visualTransformation = keywordHighlightVisualTransformation(
          keywords = mergedHighlights,
          highlightColor = Color.Yellow
        )
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        val context = LocalContext.current
        OutlinedButton(
          onClick = { copyTextToClipboard(context, entry.key, currentText) },
          enabled = currentText.isNotBlank()
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.common_copy))
        }
        Spacer(Modifier.width(8.dp))
        if (isNoTranslationNeeded) {
          OutlinedButton(onClick = onUnmarkNoTranslation) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.common_cancel))
          }
        } else {
          OutlinedButton(
            onClick = onMarkNoTranslation,
            enabled = entry.sourceValue.isNotBlank()
          ) {
            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.no_translation_needed_btn))
          }
        }
        if (entry.isModified) {
          Spacer(Modifier.width(8.dp))
          OutlinedButton(onClick = onDiscard) {
            Icon(
              painter = painterResource(id = R.drawable.ic_discard),
              contentDescription = null
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.common_cancel))
          }
        }
      }

      // TM Suggestions
      if (isFocused && tmSuggestions.isNotEmpty()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Icon(
            Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(Modifier.width(4.dp))
          Text(
            stringResource(R.string.tm_suggestion_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
        FlowRow(
          mainAxisSpacing = 6.dp,
          crossAxisSpacing = 4.dp
        ) {
          tmSuggestions.forEach { suggestion ->
            SuggestionChip(
              onClick = {
                currentText = suggestion.targetText
                onApplyTm(suggestion.targetText)
              },
              label = {
                Text(
                  "${suggestion.targetText} ${(suggestion.similarity * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              },
              colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
              )
            )
          }
        }
      }

      // Network translation suggestion
      if (networkSuggestion != null && networkSuggestion.entryKey == entry.key && networkSuggestion.results.isNotEmpty()) {
        FlowRow(
          mainAxisSpacing = 6.dp,
          crossAxisSpacing = 4.dp
        ) {
          networkSuggestion.results.forEach { result ->
            SuggestionChip(
              onClick = {
                currentText = result.translatedText
                onApplyNetwork(result.translatedText)
              },
              label = {
                Text(
                  "${result.engineName}: ${result.translatedText}",
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              },
              colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
              )
            )
          }
        }
      }

      // Translate button
      if (hasEngine && entry.sourceValue.isNotBlank()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          SuggestionChip(
            onClick = onTranslate,
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.engine_translate_button), style = MaterialTheme.typography.labelSmall)
              }
            }
          )
        }
      }
    }
  }
}

// --- Deleted Entry Card ---
@Composable
fun DeletedTranslationCard(
  entry: TranslationEntry,
  onDelete: () -> Unit
) {
  val context = LocalContext.current
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    )
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = entry.key,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        BadgeLabel(
          text = stringResource(R.string.deleted_badge),
          color = MaterialTheme.colorScheme.error,
          background = MaterialTheme.colorScheme.errorContainer
        )
      }

      // Target value (the orphaned translation)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(MaterialTheme.shapes.small)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        Text(
          text = entry.originalTargetValue,
          style = MaterialTheme.typography.bodyMedium
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        OutlinedButton(
          onClick = { copyTextToClipboard(context, entry.key, entry.originalTargetValue) },
          enabled = entry.originalTargetValue.isNotBlank()
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.common_copy), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = onDelete,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
          )
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.deleted_delete_single), style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

// --- Staged Deleted Card (A3: undo support) ---
@Composable
fun StagedDeletedCard(
  item: DeletedItem,
  onUndo: () -> Unit
) {
  val context = LocalContext.current
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = item.key,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        BadgeLabel(
          text = stringResource(R.string.deleted_staged_badge),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          background = MaterialTheme.colorScheme.surfaceVariant
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(MaterialTheme.shapes.small)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
          .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        Text(
          text = item.targetValue,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        OutlinedButton(
          onClick = { copyTextToClipboard(context, item.key, item.targetValue) },
          enabled = item.targetValue.isNotBlank()
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.common_copy), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = onUndo,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
          )
        ) {
          Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.deleted_undo), style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

// --- Diff Entry Card ---
// Shows entries where the source text changed since the dictionary was saved.
// Displays: old source (from dict), new source (current), dict translation, and editable current translation.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffTranslationCard(
  entry: TranslationEntry,
  highlightKeywords: Set<String>,
  searchHighlightQuery: String,
  isCurrentSearchResult: Boolean,
  onSave: (String) -> Unit,
  onRevertToDict: () -> Unit,
  onCalibrate: (String, String, String) -> Unit = { _, _, _ -> }
) {
  val context = LocalContext.current
  var currentText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }
  var lastCommittedText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }
  var isFocused by remember { mutableStateOf(false) }
  var isCalibrating by remember(entry.key) { mutableStateOf(false) }
  var calibratingText by remember(entry.key, entry.sourceValue) { mutableStateOf(entry.sourceValue) }
  val latestText = rememberUpdatedState(currentText)
  val latestCommittedText = rememberUpdatedState(lastCommittedText)
  val latestSave = rememberUpdatedState(onSave)
  val mergedHighlights = remember(highlightKeywords, searchHighlightQuery) {
    buildSet {
      addAll(highlightKeywords)
      if (searchHighlightQuery.isNotBlank()) add(searchHighlightQuery)
    }
  }
  val commitCurrentText = {
    if (currentText != lastCommittedText) {
      onSave(currentText)
      lastCommittedText = currentText
    }
  }

  DisposableEffect(entry.key) {
    onDispose {
      if (latestText.value != latestCommittedText.value) {
        latestSave.value(latestText.value)
      }
    }
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrentSearchResult) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
      } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
      }
    ),
    border = if (isCurrentSearchResult) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      // Key row
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = entry.key,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        BadgeLabel(
          text = stringResource(R.string.diff_badge),
          color = MaterialTheme.colorScheme.tertiary,
          background = MaterialTheme.colorScheme.tertiaryContainer
        )
      }

      // Old source text (from dictionary)
      if (entry.dictSourceValue != null) {
        Text(
          text = stringResource(R.string.diff_old_source),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.error
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Text(
            text = entry.dictSourceValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }

      // New source text (current) with calibration support
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(R.string.diff_new_source),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
          )
          if (entry.isCalibrated) {
            BadgeLabel(
              text = stringResource(R.string.calibration_badge),
              color = MaterialTheme.colorScheme.tertiary,
              background = MaterialTheme.colorScheme.tertiaryContainer
            )
            Spacer(Modifier.width(6.dp))
          }
          IconButton(
            onClick = {
              isCalibrating = !isCalibrating
              calibratingText = entry.sourceValue
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              Icons.Default.Edit,
              contentDescription = stringResource(R.string.calibration_btn),
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (isCalibrating) {
          OutlinedTextField(
            value = calibratingText,
            onValueChange = { calibratingText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.calibration_title)) },
            minLines = 2
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(onClick = { isCalibrating = false }) {
              Text(stringResource(R.string.common_cancel))
            }
            Spacer(Modifier.width(8.dp))
            Button(
              onClick = {
                onCalibrate(entry.key, entry.sourceValue, calibratingText)
                isCalibrating = false
              },
              enabled = calibratingText.isNotBlank() && calibratingText != entry.sourceValue
            ) {
              Text(stringResource(R.string.common_save))
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(horizontal = 10.dp, vertical = 8.dp)
          ) {
            HighlightedText(text = entry.sourceValue, keywords = mergedHighlights)
          }
        }
      }

      // Dictionary translation (for reference)
      if (entry.dictValue != null) {
        Text(
          text = stringResource(R.string.diff_dict_value),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.tertiary
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Text(
            text = entry.dictValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
        }
      }

      // Current translation input
      OutlinedTextField(
        value = currentText,
        onValueChange = {
          currentText = it
        },
        modifier = Modifier
          .fillMaxWidth()
          .onFocusChanged { focusState ->
            if (focusState.isFocused && !isFocused) {
              isFocused = true
            } else if (!focusState.isFocused && isFocused) {
              isFocused = false
              commitCurrentText()
            }
          },
        label = { Text(stringResource(R.string.diff_current_value)) },
        visualTransformation = keywordHighlightVisualTransformation(
          keywords = mergedHighlights,
          highlightColor = Color.Yellow
        )
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        OutlinedButton(
          onClick = { copyTextToClipboard(context, entry.key, currentText) },
          enabled = currentText.isNotBlank()
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(stringResource(R.string.common_copy), style = MaterialTheme.typography.labelMedium)
        }
        if (entry.dictValue != null) {
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = {
              currentText = entry.dictValue
              onRevertToDict()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.tertiary
            )
          ) {
            Text(stringResource(R.string.diff_revert_to_dict), style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun BadgeLabel(text: String, color: Color, background: Color) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    color = color,
    modifier = Modifier
      .clip(MaterialTheme.shapes.extraSmall)
      .background(background)
      .padding(horizontal = 6.dp, vertical = 2.dp)
  )
}

@Composable
fun HighlightedText(text: String, keywords: Set<String>, modifier: Modifier = Modifier) {
  val highlightColor = Color.Yellow
  if (keywords.isEmpty() || text.isBlank()) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodyMedium)
    return
  }

  val annotatedString = buildAnnotatedString {
    append(text)
    keywords.forEach { keyword ->
      if (keyword.isNotBlank()) {
        var startIndex = text.indexOf(keyword, ignoreCase = true)
        while (startIndex != -1) {
          val endIndex = startIndex + keyword.length
          addStyle(
            style = SpanStyle(background = highlightColor),
            start = startIndex,
            end = endIndex
          )
          startIndex = text.indexOf(keyword, startIndex + 1, ignoreCase = true)
        }
      }
    }
  }
  Text(annotatedString, modifier = modifier, style = MaterialTheme.typography.bodyMedium)
}

fun keywordHighlightVisualTransformation(keywords: Set<String>, highlightColor: Color): VisualTransformation {
  return VisualTransformation { text ->
    if (keywords.isEmpty()) {
      return@VisualTransformation TransformedText(text, OffsetMapping.Identity)
    }

    val annotatedString = buildAnnotatedString {
      append(text.text)
      keywords.forEach { keyword ->
        if (keyword.isNotBlank()) {
          var startIndex = text.text.indexOf(keyword, ignoreCase = true)
          while (startIndex != -1) {
            val endIndex = startIndex + keyword.length
            addStyle(
              style = SpanStyle(background = highlightColor),
              start = startIndex,
              end = endIndex
            )
            startIndex = text.text.indexOf(keyword, startIndex + 1, ignoreCase = true)
          }
        }
      }
    }
    TransformedText(annotatedString, OffsetMapping.Identity)
  }
}
