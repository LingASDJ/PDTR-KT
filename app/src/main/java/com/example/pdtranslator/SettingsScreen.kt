
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: TranslatorViewModel,
    onNavigateToDependencies: () -> Unit
) {
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()
    val selectedEngine by viewModel.translationEngine.collectAsState()

    if (showAboutDialog) {
        AboutDialog(onDismiss = { viewModel.setShowAboutDialog(false) })
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item { SectionTitle("通用") }
        item { ThemeColorSetting() }
        item { TranslationEngineSetting(selectedEngine) { engine -> viewModel.setTranslationEngine(engine) } }

        item { Spacer(modifier = Modifier.padding(vertical = 8.dp)) }

        item { SectionTitle("关于") }
        item { LibraryInfoSetting(onClick = onNavigateToDependencies) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationEngineSetting(selectedEngine: String, onEngineSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val engines = listOf("内置有道", "百度翻译")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("翻译引擎", style = MaterialTheme.typography.bodyLarge)
            Text("选择用于翻译的在线服务", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.menuAnchor()) {
                Text(selectedEngine)
                Icon(Icons.Default.ArrowDropDown, "")
            }
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                engines.forEach { engine ->
                    DropdownMenuItem(text = { Text(engine) }, onClick = { onEngineSelected(engine); expanded = false })
                }
            }
        }
    }
}

@Composable
fun ThemeColorSetting() {
    SettingItem(Icons.Default.Palette, "主题颜色", "自定义应用的主题颜色", onClick = { /* TODO */ })
}

@Composable
fun LibraryInfoSetting(onClick: () -> Unit) {
    SettingItem(Icons.Default.Info, "程序依赖库使用", "查看应用使用的开源库", onClick = onClick)
}

@Composable
fun AboutUsSetting(onClick: () -> Unit) {
    SettingItem(Icons.Default.Info, "关于我们", "了解本应用的开发故事", onClick = onClick)
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val repoUrl = "https://github.com/LingASDJ/PDTR-KT"

    val annotatedString = buildAnnotatedString {
        append("此应用由指挥官 JDSALing 构思和指导，由 Gemini AI 负责具体的编码实现。\n\n你的想法，我的代码，我们共同创造！\n\n")
        pushStringAnnotation(tag = "URL", annotation = repoUrl)
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
            append("我们的GitHub仓库")
        }
        pop()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于我们") },
        text = {
            Column {
                Text(annotatedString)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { uriHandler.openUri(repoUrl) }) {
                     Text("访问仓库")
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("关闭") } }
    )
}
