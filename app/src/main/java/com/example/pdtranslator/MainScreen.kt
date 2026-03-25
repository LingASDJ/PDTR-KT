package com.example.pdtranslator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pdtranslator.ui.theme.PixelBrickBackground
import com.example.pdtranslator.ui.theme.TorchGlowOverlay
import com.example.pdtranslator.ui.theme.pdNavIcons
import com.example.pdtranslator.ui.theme.rememberTimeTick

sealed class Screen(val route: String, val title: Int, val icon: ImageVector) {
    object Config : Screen("config", R.string.screen_title_config, Icons.Default.Build)
    object Translator : Screen("translator", R.string.screen_title_translator, Icons.Default.Translate)
    object Settings : Screen("settings", R.string.screen_title_settings, Icons.Default.Settings)
}

object DictionaryPreview {
    const val route = "dictionary_preview"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TranslatorViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToDependencies: () -> Unit,
    onNavigateToChangelog: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val navController = rememberNavController()
    val themeColor by viewModel.themeColor.collectAsState()
    val isPixelDungeon = themeColor == ThemeColor.PIXEL_DUNGEON

    val items = listOf(Screen.Config, Screen.Translator, Screen.Settings)
    // PD: nav icons change with time of day (tick triggers recomposition)
    val tick = if (isPixelDungeon) rememberTimeTick() else 0
    val (pdIcon0, pdIcon1, pdIcon2) = if (isPixelDungeon) pdNavIcons() else Triple(0, 0, 0)
    val pdIcons = listOf(pdIcon0, pdIcon1, pdIcon2)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = if (isPixelDungeon) MaterialTheme.colorScheme.primaryContainer
                                 else MaterialTheme.colorScheme.surface,
                contentColor = if (isPixelDungeon) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val navColors = if (isPixelDungeon) {
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    } else {
                        NavigationBarItemDefaults.colors()
                    }
                    val itemIndex = items.indexOf(screen)
                    NavigationBarItem(
                        icon = {
                            if (isPixelDungeon && itemIndex in pdIcons.indices) {
                                Icon(
                                    painter = painterResource(pdIcons[itemIndex]),
                                    contentDescription = stringResource(screen.title)
                                )
                            } else {
                                Icon(screen.icon, contentDescription = stringResource(screen.title))
                            }
                        },
                        label = { Text(stringResource(screen.title)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = navColors,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            if (isPixelDungeon) {
                // Layer 1: Brick wall
                PixelBrickBackground()
                // Layer 2: Torch glow spots (animated flicker)
                TorchGlowOverlay()
            }

            // Layer 3: Actual content
            NavHost(navController, startDestination = Screen.Config.route) {
                composable(Screen.Config.route) {
                    ConfigScreen(
                        viewModel = viewModel,
                        onNavigateToDictionaryPreview = { navController.navigate(DictionaryPreview.route) }
                    )
                }
                composable(Screen.Translator.route) {
                    TranslatorScreen(viewModel = viewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToDependencies = onNavigateToDependencies,
                        onNavigateToChangelog = onNavigateToChangelog,
                        onLanguageSelected = onLanguageSelected
                    )
                }
                composable(DictionaryPreview.route) {
                    DictionaryPreviewScreen(
                        viewModel = viewModel,
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
