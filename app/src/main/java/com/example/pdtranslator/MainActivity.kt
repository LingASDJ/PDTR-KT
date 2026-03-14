package com.example.pdtranslator

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pdtranslator.ui.theme.PDTranslatorTheme
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()

    private val saveLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { saveModifiedContent(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            PDTranslatorTheme {
                MainApp(viewModel = viewModel, onSave = { onSave() })
            }
        }
    }

    private fun onSave() {
        val targetLanguage = viewModel.targetLanguage.value ?: return
        val fileName = targetLanguage.fileName
        saveLauncher.launch(fileName)
    }

    private fun saveModifiedContent(uri: Uri) {
        val content = viewModel.getModifiedContentForTarget() ?: return
        try {
            contentResolver.openOutputStream(uri)?.use {
                OutputStreamWriter(it).use { writer -> writer.write(content) }
            }
        } catch (e: Exception) {
            // Handle exceptions
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Translator : Screen("translator", "Translator", Icons.Default.Translate)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: TranslatorViewModel, onSave: () -> Unit) {
    val navController = rememberNavController()

    val items = listOf(
        Screen.Translator,
        Screen.Settings,
    )

    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Translator.route, Modifier.padding(innerPadding)) {
            composable(Screen.Translator.route) {
                TranslatorNavigation(viewModel = viewModel, onSave = onSave)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateToChangelog = { navController.navigate("changelog") })
            }
            composable("changelog") {
                ChangelogScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun TranslatorNavigation(viewModel: TranslatorViewModel, onSave: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val openLanguageGroupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                viewModel.loadLanguageFiles(context.contentResolver, uris)
                navController.navigate("languageGroupSelector")
            }
        }
    )

    val openSingleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.loadLanguageFiles(context.contentResolver, listOf(it))
                navController.navigate("languageGroupSelector")
            }
        }
    )

    NavHost(navController = navController, startDestination = "translatorMain") {
        composable("translatorMain") {
            TranslatorScreen(
                viewModel = viewModel,
                onSelectSingleFile = { openSingleFileLauncher.launch(arrayOf("*/*")) },
                onSelectLanguageGroup = { openLanguageGroupLauncher.launch(arrayOf("*/*")) },
                onSave = onSave
            )
        }
        composable("languageGroupSelector") {
            LanguageGroupScreen(
                viewModel = viewModel,
                onGroupSelected = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
