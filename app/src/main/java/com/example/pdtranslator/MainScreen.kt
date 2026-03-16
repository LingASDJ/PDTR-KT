package com.example.pdtranslator

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val title: Int, val icon: ImageVector) {
    object Config : Screen("config", R.string.screen_title_config, Icons.Default.Build)
    object Translator : Screen("translator", R.string.screen_title_translator, Icons.Default.Translate)
    object Settings : Screen("settings", R.string.screen_title_settings, Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TranslatorViewModel,
    onNavigateToDependencies: () -> Unit,
    onNavigateToChangelog: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onShowSnackbar: suspend (String) -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        Screen.Config,
        Screen.Translator,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(id = screen.title)) },
                        label = { Text(stringResource(id = screen.title)) },
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
        NavHost(
            navController, 
            startDestination = Screen.Config.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Config.route) {
                 ConfigScreen(viewModel = viewModel)
            }
            composable(Screen.Translator.route) {
                TranslatorScreen(viewModel = viewModel, onShowSnackbar = onShowSnackbar)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToDependencies = onNavigateToDependencies,
                    onNavigateToChangelog = onNavigateToChangelog,
                    onLanguageSelected = onLanguageSelected
                )
            }
        }
    }
}
