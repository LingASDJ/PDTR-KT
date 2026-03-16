package com.example.pdtranslator

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    paddingValues: PaddingValues,
    onLanguageSelected: (String) -> Unit,
    onShowSnackbar: suspend (String) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = AppDestinations.MAIN_SCREEN,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(AppDestinations.MAIN_SCREEN) {
             MainScreen(
                viewModel = viewModel,
                onNavigateToDependencies = { navController.navigate(AppDestinations.DEPENDENCY_SCREEN) },
                onNavigateToChangelog = { navController.navigate(AppDestinations.CHANGELOG_SCREEN) },
                onLanguageSelected = onLanguageSelected,
                onShowSnackbar = onShowSnackbar
            )
        }
        composable(AppDestinations.DEPENDENCY_SCREEN) {
            DependencyScreen(onNavigateUp = { navController.popBackStack() })
        }
        composable(AppDestractions.CHANGELOG_SCREEN) {
            ChangelogScreen()
        }
    }
}
