package com.example.pdtranslator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.pdtranslator.engine.EngineConfig
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun SettingsScreen(
    viewModel: TranslatorViewModel,
    onNavigateToDependencies: () -> Unit,
    onNavigateToChangelog: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()
    val selectedTheme by viewModel.themeColor.collectAsState()
    val preserveOriginalFormatOnExport by viewModel.preserveOriginalFormatOnExport.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showEngineDialog by remember { mutableStateOf(false) }

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
    if (showEngineDialog) {
        TranslationEngineDialog(
            viewModel = viewModel,
            onDismiss = { showEngineDialog = false }
        )
    }

    val isPD = selectedTheme == ThemeColor.PIXEL_DUNGEON

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { SectionTitle(stringResource(R.string.settings_section_general)) }
        item { LanguageSetting(isPD) { showLanguageDialog = true } }
        item { ThemeColorSetting(isPD) { showThemeColorDialog = true } }
        item { TranslationEngineSetting(isPD) { showEngineDialog = true } }
        item {
            ExportPreserveFormatSetting(
                isPD = isPD,
                checked = preserveOriginalFormatOnExport,
                onCheckedChange = viewModel::setPreserveOriginalFormatOnExport
            )
        }

        item { Spacer(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionTitle(stringResource(R.string.settings_section_about)) }
        item { LibraryInfoSetting(isPD, onClick = onNavigateToDependencies) }
        item { ChangelogSetting(isPD, onClick = onNavigateToChangelog) }
        item { AboutUsSetting(isPD) { viewModel.setShowAboutDialog(true) } }

        // PD easter egg: dungeon depth info
        if (isPD) {
            item { Spacer(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SectionTitle(stringResource(R.string.pd_dungeon_section)) }
            item {
                val tick = rememberSettingsTimeTick()
                val hour = tick / 60
                val zoneName = settingsZoneName(hour)
                val depth = (hour + 1).coerceIn(1, 26)
                SettingItemPd(
                    iconRes = R.drawable.ic_pd_map,
                    title = stringResource(R.string.pd_current_zone, zoneName),
                    subtitle = stringResource(R.string.pd_depth, depth),
                    onClick = {}
                )
            }
        }
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
fun SettingItemPd(iconRes: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(iconRes), contentDescription = title, modifier = Modifier.padding(end = 16.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun rememberSettingsTimeTick(): Int {
    val cal = Calendar.getInstance()
    var tick by remember { mutableStateOf(cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            val now = Calendar.getInstance()
            tick = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        }
    }
    return tick
}

private fun settingsZoneName(hour: Int): String {
    return when (hour) {
        0 -> "恶魔大厅深层 Deep Demon Halls"
        1 -> "燃烧拳魔 Burning Fist"
        2 -> "腐烂拳魔 Rotting Fist"
        3 -> "冰霜拳魔 Ice Fist"
        4 -> "下水道 Sewers"
        5 -> "下水道BOSS Goo"
        6 -> "花园 Garden"
        7 -> "监狱 Prison"
        8 -> "监狱牢房 Prison Cells"
        9 -> "天狗 Tengu"
        10 -> "洞窟 Caves"
        11 -> "矿洞深处 Deep Mines"
        12 -> "水晶洞窟 Crystal Caves"
        13 -> "矮人城 Dwarf City"
        14 -> "矮人城中心 City Center"
        15 -> "矮人国王 Dwarf King"
        16 -> "恶魔大厅 Demon Halls"
        17 -> "魔眼层 Evil Eyes"
        18 -> "蝎子巢窟 Scorpio Nest"
        19 -> "犹格索托斯 Yog-Dzewa"
        20 -> "光明拳魔 Bright Fist"
        21 -> "暗影拳魔 Dark Fist"
        22 -> "锈蚀拳魔 Rusted Fist"
        23 -> "最终之层 Last Level"
        else -> "地牢 Dungeon"
    }
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun SettingSwitchItemPd(
    iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.padding(end = 16.dp).size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
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
                            ThemeColor.MODERN -> stringResource(id = R.string.theme_name_modern)
                            ThemeColor.PIXEL_DUNGEON -> stringResource(id = R.string.theme_name_pixel_dungeon)
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
fun LanguageSetting(isPD: Boolean = false, onClick: () -> Unit) {
    if (isPD) SettingItemPd(R.drawable.ic_pd_ring, stringResource(R.string.settings_item_language), stringResource(R.string.settings_item_language_subtitle), onClick)
    else SettingItem(Icons.Default.Language, stringResource(R.string.settings_item_language), stringResource(R.string.settings_item_language_subtitle), onClick)
}

@Composable
fun ThemeColorSetting(isPD: Boolean = false, onClick: () -> Unit) {
    if (isPD) SettingItemPd(R.drawable.ic_pd_armor, stringResource(R.string.settings_item_theme_color), stringResource(R.string.settings_item_theme_color_subtitle), onClick)
    else SettingItem(Icons.Default.Palette, stringResource(R.string.settings_item_theme_color), stringResource(R.string.settings_item_theme_color_subtitle), onClick)
}

@Composable
fun TranslationEngineSetting(isPD: Boolean = false, onClick: () -> Unit) {
    if (isPD) SettingItemPd(R.drawable.ic_pd_wand, stringResource(R.string.settings_item_translation_engine), stringResource(R.string.settings_item_translation_engine_subtitle), onClick)
    else SettingItem(Icons.Default.Translate, stringResource(R.string.settings_item_translation_engine), stringResource(R.string.settings_item_translation_engine_subtitle), onClick)
}

@Composable
fun ExportPreserveFormatSetting(
    isPD: Boolean = false,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    if (isPD) {
        SettingSwitchItemPd(
            iconRes = R.drawable.ic_pd_scroll,
            title = stringResource(R.string.settings_item_export_preserve_format),
            subtitle = stringResource(R.string.settings_item_export_preserve_format_subtitle),
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    } else {
        SettingSwitchItem(
            icon = Icons.Default.Description,
            title = stringResource(R.string.settings_item_export_preserve_format),
            subtitle = stringResource(R.string.settings_item_export_preserve_format_subtitle),
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationEngineDialog(
    viewModel: TranslatorViewModel,
    onDismiss: () -> Unit
) {
    val engineManager = viewModel.engineManager
    val engines = engineManager.availableEngines
    var selectedId by remember { mutableStateOf(engineManager.getSelectedEngineId()) }
    var apiKey by remember(selectedId) { mutableStateOf(engineManager.getApiKey(selectedId)) }
    var endpoint by remember(selectedId) { mutableStateOf(engineManager.getEndpoint(selectedId)) }
    var showApiKey by remember(selectedId) { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selectedConfig = engines.find { it.id == selectedId }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title
                Text(
                    stringResource(R.string.engine_select_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Engine selection
                    engines.forEach { engine ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (engine.id == selectedId),
                                    onClick = {
                                        selectedId = engine.id
                                        engineManager.setSelectedEngine(engine.id)
                                        apiKey = engineManager.getApiKey(engine.id)
                                        endpoint = engineManager.getEndpoint(engine.id)
                                        testResult = null
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (engine.id == selectedId), onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(engine.nameResId), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // None option
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedId.isBlank(),
                                onClick = {
                                    selectedId = ""
                                    engineManager.setSelectedEngine("")
                                    testResult = null
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedId.isBlank(), onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.engine_select_none), style = MaterialTheme.typography.bodyMedium)
                    }

                    // Config fields for selected engine
                    if (selectedConfig != null) {
                        Spacer(Modifier.height(4.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))

                        if (selectedConfig.requiresApiKey) {
                            val hint = when (selectedConfig.id) {
                                "baidu" -> stringResource(R.string.engine_api_key_hint_baidu)
                                "youdao_api" -> stringResource(R.string.engine_api_key_hint_youdao)
                                "microsoft" -> stringResource(R.string.engine_api_key_hint_microsoft)
                                else -> ""
                            }
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    engineManager.setApiKey(selectedId, it)
                                    testResult = null
                                },
                                label = { Text(stringResource(R.string.engine_api_key_label)) },
                                placeholder = if (hint.isNotBlank()) {{ Text(hint) }} else null,
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (selectedConfig.requiresEndpoint) {
                            OutlinedTextField(
                                value = endpoint,
                                onValueChange = {
                                    endpoint = it
                                    engineManager.setEndpoint(selectedId, it)
                                    testResult = null
                                },
                                label = { Text(stringResource(R.string.engine_endpoint_label)) },
                                placeholder = { Text(stringResource(R.string.engine_endpoint_hint_deeplx)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (selectedConfig.id == "deeplx") {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.engine_deeplx_http_note),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // Test connection button + inline result
                        val testSuccessText = stringResource(R.string.engine_test_success_short)
                        val testFailText = stringResource(R.string.engine_test_fail_short)
                        val testingText = stringResource(R.string.engine_testing)
                        OutlinedButton(
                            onClick = {
                                isTesting = true
                                testResult = null
                                scope.launch {
                                    val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        engineManager.testConnection()
                                    }
                                    isTesting = false
                                    testResult = if (result.isSuccess) {
                                        "$testSuccessText ${result.getOrThrow()}"
                                    } else {
                                        "$testFailText ${engineManager.getFriendlyError(selectedId, result.exceptionOrNull())}"
                                    }
                                }
                            },
                            enabled = !isTesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(testingText)
                            } else {
                                Text(stringResource(R.string.engine_test_connection))
                            }
                        }

                        // Show result inline
                        if (testResult != null) {
                            Spacer(Modifier.height(8.dp))
                            val isSuccess = testResult!!.startsWith(testSuccessText)
                            Text(
                                text = testResult!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    )
                                    .padding(8.dp)
                            )
                        }

                    }
                }

                // Close button
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_close_button))
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryInfoSetting(isPD: Boolean = false, onClick: () -> Unit) {
    if (isPD) SettingItemPd(R.drawable.ic_pd_scroll, stringResource(R.string.settings_item_source_code_license), stringResource(R.string.settings_item_source_code_license_subtitle), onClick)
    else SettingItem(Icons.Default.Info, stringResource(R.string.settings_item_source_code_license), stringResource(R.string.settings_item_source_code_license_subtitle), onClick)
}

@Composable
fun ChangelogSetting(isPD: Boolean = false, onClick: () -> Unit) {
    if (isPD) SettingItemPd(R.drawable.ic_pd_map, stringResource(R.string.settings_item_changelog), stringResource(R.string.settings_item_changelog_subtitle), onClick)
    else SettingItem(Icons.Default.Info, stringResource(R.string.settings_item_changelog), stringResource(R.string.settings_item_changelog_subtitle), onClick)
}

@Composable
fun AboutUsSetting(isPD: Boolean = false, onClick: () -> Unit) {
    if (isPD) SettingItemPd(R.drawable.ic_pd_skull, stringResource(R.string.settings_item_about_us), stringResource(R.string.settings_item_about_us_subtitle), onClick)
    else SettingItem(Icons.Default.Info, stringResource(R.string.settings_item_about_us), stringResource(R.string.settings_item_about_us_subtitle), onClick)
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
