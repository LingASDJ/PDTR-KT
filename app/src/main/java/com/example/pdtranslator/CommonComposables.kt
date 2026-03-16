package com.example.pdtranslator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun FilterButtons(selectedFilter: FilterState, onFilterSelected: (FilterState) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterState.values().forEach { state ->
            val button: @Composable () -> Unit = {
                Text(stringResource(id = when (state) {
                    FilterState.ALL -> R.string.filter_all
                    FilterState.UNTRANSLATED -> R.string.filter_untranslated
                    FilterState.TRANSLATED -> R.string.filter_translated
                    FilterState.MODIFIED -> R.string.filter_modified
                    FilterState.MISSING -> R.string.filter_missing
                }))
            }
            if (state == selectedFilter) {
                Button(onClick = { onFilterSelected(state) }, modifier = Modifier.weight(1f), content = { button() })
            } else {
                OutlinedButton(onClick = { onFilterSelected(state) }, modifier = Modifier.weight(1f), content = { button() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageGroupSelector(
    groupNames: List<String>,
    selectedGroupName: String?,
    onGroupSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            readOnly = true,
            value = selectedGroupName ?: stringResource(id = R.string.common_select_language_group),
            onValueChange = {},
            label = { Text(stringResource(id = R.string.common_language_group)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            groupNames.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onGroupSelected(name); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectors(
    availableLanguages: List<String>,
    sourceLangCode: String?,
    targetLangCode: String?,
    onSourceSelected: (String) -> Unit,
    onTargetSelected: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            LanguageSelector(availableLanguages, sourceLangCode, stringResource(id = R.string.config_source_language), onSourceSelected)
        }
        Box(modifier = Modifier.weight(1f)) {
            LanguageSelector(availableLanguages, targetLangCode, stringResource(id = R.string.config_target_language), onTargetSelected)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    languages: List<String>,
    selectedLanguage: String?,
    label: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            readOnly = true,
            value = selectedLanguage ?: "",
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { lang ->
                DropdownMenuItem(text = { Text(lang) }, onClick = { onLanguageSelected(lang); expanded = false })
            }
        }
    }
}

@Composable
fun PaginationControls(currentPage: Int, totalPages: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(onClick = onPrevious, enabled = currentPage > 1) {
            Text(stringResource(id = R.string.pagination_previous))
        }
        Text(stringResource(id = R.string.pagination_page_info, currentPage, totalPages), modifier = Modifier.weight(1f))
        Button(onClick = onNext, enabled = currentPage < totalPages) {
            Text(stringResource(id = R.string.pagination_next))
        }
    }
}
