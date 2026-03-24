package com.example.pdtranslator

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object AppDestinations {
    const val MAIN_SCREEN = "main"
    const val DEPENDENCY_SCREEN = "dependencies"
    const val CHANGELOG_SCREEN = "changelog"
}

@Composable
fun AppNavigator(
    viewModel: TranslatorViewModel,
    snackbarHostState: SnackbarHostState,
    onLanguageSelected: (String) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestinations.MAIN_SCREEN
    ) {
        composable(AppDestinations.MAIN_SCREEN) {
             MainScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onNavigateToDependencies = { navController.navigate(AppDestinations.DEPENDENCY_SCREEN) },
                onNavigateToChangelog = { navController.navigate(AppDestinations.CHANGELOG_SCREEN) },
                onLanguageSelected = onLanguageSelected
            )
        }
        composable(AppDestinations.DEPENDENCY_SCREEN) {
            DependencyScreen(onNavigateUp = { navController.popBackStack() })
        }
        composable(AppDestinations.CHANGELOG_SCREEN) {
            ChangelogScreen()
        }
    }
}
