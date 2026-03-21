package com.example.pdtranslator

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.ui.platform.LocalContext

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
  val context = LocalContext.current

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
          PaginationControls(deletedPage, deletedTotalPages, viewModel::previousPage, viewModel::nextPage)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(displayEntries, key = { it.key }) { entry ->
              DiffTranslationCard(
                entry = entry,
                highlightKeywords = highlightKeywords,
                onSave = { newText -> viewModel.stageChange(entry.key, newText) },
                onRevertToDict = {
                  entry.dictValue?.let { viewModel.stageChange(entry.key, it) }
                }
              )
            }
          }
          PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage)
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
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(displayEntries, key = { it.key }) { entry ->
            NewTranslationCard(
              entry = entry,
              highlightKeywords = highlightKeywords,
              tmSuggestions = tmSuggestions,
              networkSuggestion = networkSuggestion,
              onSave = { newText -> viewModel.stageChange(entry.key, newText) },
              onDiscard = { viewModel.unstageChange(entry.key) },
              onFocused = { viewModel.requestTmSuggestions(entry.sourceValue) },
              onUnfocused = { viewModel.clearTmSuggestions() },
              onApplyTm = { targetText -> viewModel.applyTmSuggestion(entry.key, targetText) },
              onTranslate = { viewModel.translateEntry(entry.key, entry.sourceValue) },
              onApplyNetwork = { text -> viewModel.applyNetworkSuggestion(entry.key, text) },
              hasEngine = viewModel.engineManager.getSelectedEngineId().isNotBlank()
            )
          }
        }
        PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage)
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
  val context = LocalContext.current

  Card {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Spacer(modifier = Modifier.weight(1f))
        // Find button — triggers search filter
        Button(
          onClick = { viewModel.setFilter(FilterState.ALL) },
          modifier = Modifier.height(32.dp),
          contentPadding = ButtonDefaults.TextButtonContentPadding,
          enabled = searchQuery.isNotBlank()
        ) {
          Text(stringResource(R.string.search_find_btn), style = MaterialTheme.typography.labelSmall)
        }
        // Replace All button
        Button(
          onClick = {
            val count = viewModel.replaceAllMatching()
            android.widget.Toast.makeText(
              context,
              context.getString(R.string.search_replace_done, count),
              android.widget.Toast.LENGTH_SHORT
            ).show()
          },
          modifier = Modifier.height(32.dp),
          contentPadding = ButtonDefaults.TextButtonContentPadding,
          enabled = searchQuery.isNotBlank() && replaceQuery.isNotEmpty()
        ) {
          Text(stringResource(R.string.search_replace_btn), style = MaterialTheme.typography.labelSmall)
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
  tmSuggestions: List<TmSuggestion>,
  networkSuggestion: NetworkSuggestionState?,
  onSave: (String) -> Unit,
  onDiscard: () -> Unit,
  onFocused: () -> Unit,
  onUnfocused: () -> Unit,
  onApplyTm: (String) -> Unit,
  onTranslate: () -> Unit = {},
  onApplyNetwork: (String) -> Unit = {},
  hasEngine: Boolean = false
) {
  var currentText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }
  var isFocused by remember { mutableStateOf(false) }

  val cardColors = if (entry.isModified) {
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
  } else {
    CardDefaults.cardColors()
  }

  Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
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
      }

      // Source text in a subtle container
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        HighlightedText(text = entry.sourceValue, keywords = highlightKeywords)
      }

      // Translation input
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = currentText,
          onValueChange = { currentText = it },
          modifier = Modifier
            .weight(1f)
            .onFocusChanged { focusState ->
              if (focusState.isFocused && !isFocused) {
                isFocused = true
                onFocused()
              } else if (!focusState.isFocused && isFocused) {
                isFocused = false
                if (currentText != entry.targetValue) {
                  onSave(currentText)
                }
                onUnfocused()
              }
            },
          label = { Text(stringResource(id = R.string.common_translation)) },
          visualTransformation = keywordHighlightVisualTransformation(
            keywords = highlightKeywords,
            highlightColor = Color.Yellow
          )
        )

        if (entry.isModified) {
          IconButton(onClick = onDiscard) {
            Icon(
              painter = painterResource(id = R.drawable.ic_discard),
              contentDescription = "Discard Changes"
            )
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
  Card(
    modifier = Modifier.fillMaxWidth(),
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
          .clip(RoundedCornerShape(8.dp))
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
  Card(
    modifier = Modifier.fillMaxWidth(),
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
          .clip(RoundedCornerShape(8.dp))
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
  onSave: (String) -> Unit,
  onRevertToDict: () -> Unit
) {
  var currentText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    )
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
            .clip(RoundedCornerShape(8.dp))
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

      // New source text (current)
      Text(
        text = stringResource(R.string.diff_new_source),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(horizontal = 10.dp, vertical = 8.dp)
      ) {
        HighlightedText(text = entry.sourceValue, keywords = highlightKeywords)
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
            .clip(RoundedCornerShape(8.dp))
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
        onValueChange = { currentText = it },
        modifier = Modifier
          .fillMaxWidth()
          .onFocusChanged { focusState ->
            if (!focusState.isFocused && currentText != entry.targetValue) {
              onSave(currentText)
            }
          },
        label = { Text(stringResource(R.string.diff_current_value)) },
        visualTransformation = keywordHighlightVisualTransformation(
          keywords = highlightKeywords,
          highlightColor = Color.Yellow
        )
      )

      // Revert to dictionary button
      if (entry.dictValue != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
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
      .clip(RoundedCornerShape(4.dp))
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
