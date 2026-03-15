package com.example.pdtranslator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageGroupSelector(groups: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected ?: stringResource(id = R.string.common_select_language_group),
            onValueChange = {},
            label = { Text(stringResource(id = R.string.common_language_group)) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            groups.forEach { group ->
                DropdownMenuItem(text = { Text(group) }, onClick = { onSelect(group); expanded = false })
            }
        }
    }
}

@Composable
fun getLanguageDisplayName(langCode: String): String {
    if (langCode.isEmpty()) return ""
    if (langCode == "base") return stringResource(id = R.string.source_language)

    // Handle 'chk' as a special case for Traditional Chinese
    val bcp47Tag = when (langCode.lowercase()) {
        "chk" -> "zh-Hant"
        else -> langCode
    }

    val currentLocale = Locale.getDefault()
    val languageLocale = Locale.forLanguageTag(bcp47Tag)

    val nativeName = languageLocale.getDisplayName(languageLocale)
    val localizedName = languageLocale.getDisplayName(currentLocale)

    // Capitalize the first letter for consistency
    val finalNativeName = nativeName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(languageLocale) else it.toString() }
    val finalLocalizedName = localizedName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString() }

    return if (finalNativeName.equals(finalLocalizedName, ignoreCase = true)) {
        finalNativeName
    } else {
        "$finalNativeName ($finalLocalizedName)"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.LanguageSelector(label: String, languages: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.weight(1f)) {
        OutlinedTextField(
            value = getLanguageDisplayName(selected ?: ""),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { lang ->
                DropdownMenuItem(text = { Text(getLanguageDisplayName(lang)) }, onClick = { onSelect(lang); expanded = false })
            }
        }
    }
}

@Composable
fun FilterButtons(selectedFilter: FilterState, onFilterSelected: (FilterState) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterState.values().forEach { filter ->
            val filterName = when (filter) {
                FilterState.ALL -> stringResource(id = R.string.filter_all)
                FilterState.UNTRANSLATED -> stringResource(id = R.string.filter_untranslated)
                FilterState.TRANSLATED -> stringResource(id = R.string.filter_translated)
                FilterState.MODIFIED -> stringResource(id = R.string.filter_modified)
                FilterState.MISSING -> stringResource(id = R.string.filter_missing)
            }
            Row(
                Modifier
                    .selectable(
                        selected = (filter == selectedFilter), 
                        onClick = { onFilterSelected(filter) }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (filter == selectedFilter), 
                    onClick = { onFilterSelected(filter) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = filterName)
            }
        }
    }
}

@Composable
fun TranslationCard(entry: TranslationEntry, onValueChange: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.key, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(entry.sourceValue, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = entry.targetValue,
                onValueChange = onValueChange,
                label = { Text(stringResource(id = R.string.common_translation)) },
                modifier = Modifier.fillMaxWidth(),
                // Disable editing for missing entries until they are added
                enabled = !entry.isMissing
            )
        }
    }
}

@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onPrev, enabled = currentPage > 1) { Text(stringResource(id = R.string.pagination_previous)) }
        Spacer(modifier = Modifier.width(16.dp))
        Text(stringResource(id = R.string.pagination_page_info, currentPage, totalPages))
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = onNext, enabled = currentPage < totalPages) { Text(stringResource(id = R.string.pagination_next)) }
    }
}
