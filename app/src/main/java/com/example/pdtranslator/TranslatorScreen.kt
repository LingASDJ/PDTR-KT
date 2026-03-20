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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
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
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TranslatorScreen(viewModel: TranslatorViewModel, onShowSnackbar: suspend (String) -> Unit) {
  val displayEntries by viewModel.displayEntries.collectAsState()
  val filterState by viewModel.filterState.collectAsState()
  val currentPage by viewModel.currentPage.collectAsState()
  val totalPages by viewModel.totalPages.collectAsState()
  val infoBarText by viewModel.infoBarText.collectAsState()
  val isSearchCardVisible by viewModel.isSearchCardVisible.collectAsState()
  val missingEntriesCount by viewModel.missingEntriesCount.collectAsState()
  val highlightKeywords by viewModel.highlightKeywords.collectAsState()
  val tmSuggestions by viewModel.tmSuggestions.collectAsState()
  val languageGroupNames by viewModel.languageGroupNames.collectAsState()
  val selectedGroupName by viewModel.selectedGroupName.collectAsState()
  val sourceLangCode by viewModel.sourceLangCode.collectAsState()
  val targetLangCode by viewModel.targetLangCode.collectAsState()
  val availableLanguages by viewModel.availableLanguages.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(key1 = true) {
    viewModel.uiEvents.collectLatest {
      when (it) {
        is UiEvent.ShowSnackbar -> onShowSnackbar(it.message)
      }
    }
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

    // Info bar + toggle
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

    FilterButtons(filterState) { viewModel.setFilter(it) }

    if (filterState == FilterState.MISSING) {
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
    } else if (displayEntries.isEmpty()) {
      // Empty filter result
      EmptyState(
        icon = null,
        message = stringResource(R.string.empty_no_entries)
      )
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(displayEntries, key = { it.key }) { entry ->
          NewTranslationCard(
            entry = entry,
            highlightKeywords = highlightKeywords,
            tmSuggestions = tmSuggestions,
            onSave = { newText -> viewModel.stageChange(entry.key, newText) },
            onDiscard = { viewModel.unstageChange(entry.key) },
            onFocused = { viewModel.requestTmSuggestions(entry.sourceValue) },
            onUnfocused = { viewModel.clearTmSuggestions() },
            onApplyTm = { targetText -> viewModel.applyTmSuggestion(entry.key, targetText) }
          )
        }
      }
      PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
  onSave: (String) -> Unit,
  onDiscard: () -> Unit,
  onFocused: () -> Unit,
  onUnfocused: () -> Unit,
  onApplyTm: (String) -> Unit
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
