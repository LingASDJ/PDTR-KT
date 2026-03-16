package com.example.pdtranslator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest {
            when (it) {
                is UiEvent.ShowSnackbar -> onShowSnackbar(it.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp)) // Top padding

        AnimatedVisibility(visible = isSearchCardVisible) {
            SearchReplaceControls(viewModel)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = infoBarText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (isSearchCardVisible) "Collapse Search" else "Expand Search",
                modifier = Modifier.clickable { viewModel.toggleSearchCardVisibility() }
            )
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
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayEntries, key = { it.key }) { entry ->
                    NewTranslationCard(
                        entry = entry,
                        highlightKeywords = highlightKeywords,
                        onSave = { newText -> viewModel.stageChange(entry.key, newText) },
                        onDiscard = { viewModel.unstageChange(entry.key) }
                    )
                }
            }
            PaginationControls(currentPage, totalPages, viewModel::previousPage, viewModel::nextPage)
        }

        Spacer(modifier = Modifier.height(4.dp)) // Bottom padding
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("搜索") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { viewModel.setReplaceQuery(it) },
                    label = { Text("替换") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.selectable(
                        selected = isCaseSensitive,
                        onClick = { viewModel.setCaseSensitive(!isCaseSensitive) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isCaseSensitive, onCheckedChange = { viewModel.setCaseSensitive(it) })
                    Text("区分大小写")
                }
                Row(
                    Modifier.selectable(
                        selected = isExactMatch,
                        onClick = { viewModel.setExactMatch(!isExactMatch) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isExactMatch, onCheckedChange = { viewModel.setExactMatch(it) })
                    Text("完全匹配")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun NewTranslationCard(
    entry: TranslationEntry,
    highlightKeywords: Set<String>,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var currentText by remember(entry.key, entry.targetValue) { mutableStateOf(entry.targetValue) }

    val cardColors = if (entry.isModified) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                if (entry.isIdentical) {
                    Text(
                        text = stringResource(id = R.string.translator_identical_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (entry.isMissing) {
                    Text(
                        text = "(Missing)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                HighlightedText(text = entry.sourceValue, keywords = highlightKeywords)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged {
                                if (!it.isFocused && currentText != entry.targetValue) {
                                    onSave(currentText)
                                }
                            },
                        label = { Text(stringResource(id = R.string.common_translation)) },
                        visualTransformation = keywordHighlightVisualTransformation(
                            keywords = highlightKeywords,
                            highlightColor = MaterialTheme.colorScheme.primaryContainer
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
            }
        }
    }
}

@Composable
fun HighlightedText(text: String, keywords: Set<String>, modifier: Modifier = Modifier) {
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
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
