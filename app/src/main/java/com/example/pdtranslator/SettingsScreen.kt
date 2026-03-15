package com.example.pdtranslator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: TranslatorViewModel,
    onNavigateToDependencies: () -> Unit,
    onNavigateToChangelog: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()
    val selectedEngine by viewModel.translationEngine.collectAsState()
    val selectedTheme by viewModel.themeColor.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { viewModel.setShowAboutDialog(false) })
    }
    if (showLanguageDialog) {
        LanguageDialog(onDismiss = { showLanguageDialog = false }, onLanguageSelected = onLanguageSelected)
    }
    if (showThemeColorDialog) {
        ThemeColorSelectorDialog(
            currentTheme = selectedTheme,
            onDismiss = { showThemeColorDialog = false },
            onThemeSelected = { theme ->
                viewModel.setThemeColor(theme)
                showThemeColorDialog = false
            }
        )
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { SectionTitle(stringResource(R.string.settings_section_general)) }
        item { LanguageSetting { showLanguageDialog = true } }
        item { ThemeColorSetting { showThemeColorDialog = true } }
        item { TranslationEngineSetting(selectedEngine) { engine -> viewModel.setTranslationEngine(engine) } }

        item { Spacer(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionTitle(stringResource(R.string.settings_section_about)) }
        item { LibraryInfoSetting(onClick = onNavigateToDependencies) }
        item { ChangelogSetting(onClick = onNavigateToChangelog) }
        item { AboutUsSetting { viewModel.setShowAboutDialog(true) } }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, modifier = Modifier.padding(end = 16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ThemeColorSelectorDialog(
    currentTheme: ThemeColor,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeColor) -> Unit
) {
    val themes = ThemeColor.values()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.settings_item_theme_color)) },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (theme == currentTheme),
                                onClick = { onThemeSelected(theme) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null // Click is handled by the Row
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(text = when (theme) {
                            ThemeColor.DEFAULT -> stringResource(id = R.string.theme_name_default)
                            ThemeColor.M3 -> stringResource(id = R.string.theme_name_m3)
                            ThemeColor.GREEN -> stringResource(id = R.string.theme_name_green)
                            ThemeColor.LAVENDER -> stringResource(id = R.string.theme_name_lavender)
                        })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close_button))
            }
        }
    )
}

@Composable
fun LanguageSetting(onClick: () -> Unit) {
    SettingItem(
        icon = Icons.Default.Language,
        title = stringResource(R.string.settings_item_language),
        subtitle = stringResource(R.string.settings_item_language_subtitle),
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationEngineSetting(selectedEngine: Int, onEngineSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val engines = listOf(
        R.string.translation_engine_youdao,
        R.string.translation_engine_baidu
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(stringResource(R.string.settings_item_translation_engine), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.settings_item_translation_engine_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.menuAnchor()) {
                Text(stringResource(id = selectedEngine))
                Icon(Icons.Default.ArrowDropDown, "")
            }
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                engines.forEach { engineResId ->
                    DropdownMenuItem(text = { Text(stringResource(id = engineResId)) }, onClick = { onEngineSelected(engineResId); expanded = false })
                }
            }
        }
    }
}

@Composable
fun ThemeColorSetting(onClick: () -> Unit) {
    SettingItem(
        Icons.Default.Palette,
        stringResource(R.string.settings_item_theme_color),
        stringResource(R.string.settings_item_theme_color_subtitle),
        onClick = onClick
    )
}

@Composable
fun LibraryInfoSetting(onClick: () -> Unit) {
    SettingItem(Icons.Default.Info, stringResource(R.string.settings_item_source_code_license), stringResource(R.string.settings_item_source_code_license_subtitle), onClick = onClick)
}

@Composable
fun ChangelogSetting(onClick: () -> Unit) {
    SettingItem(
        icon = Icons.Default.Info,
        title = stringResource(R.string.settings_item_changelog),
        subtitle = stringResource(R.string.settings_item_changelog_subtitle),
        onClick = onClick
    )
}

@Composable
fun AboutUsSetting(onClick: () -> Unit) {
    SettingItem(Icons.Default.Info, stringResource(R.string.settings_item_about_us), stringResource(R.string.settings_item_about_us_subtitle), onClick = onClick)
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val repoUrl = "https://github.com/LingASDJ/PDTR-KT"

    val annotatedString = buildAnnotatedString {
        append(stringResource(R.string.about_us_dialog_content))
        pushStringAnnotation(tag = "URL", annotation = repoUrl)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
            append(stringResource(R.string.about_us_dialog_repo_link))
        }
        pop()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_us_dialog_title)) },
        text = {
            Column {
                Text(annotatedString)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { uriHandler.openUri(repoUrl) }) {
                     Text(stringResource(R.string.about_us_dialog_visit_repo_button))
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.dialog_close_button)) } }
    )
}

@Composable
fun LanguageDialog(onDismiss: () -> Unit, onLanguageSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.language_chinese),
                    modifier = Modifier.fillMaxWidth().clickable { onLanguageSelected("zh"); onDismiss() }.padding(vertical = 8.dp)
                )
                Text(
                    text = stringResource(R.string.language_english),
                    modifier = Modifier.fillMaxWidth().clickable { onLanguageSelected("en"); onDismiss() }.padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close_button))
            }
        }
    )
}
