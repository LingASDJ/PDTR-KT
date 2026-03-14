package com.example.pdtranslator

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MainScreen(
    viewModel: TranslatorViewModel,
    onSelectLanguageGroup: () -> Unit,
    onSave: () -> Unit
) {
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()
    val items = listOf("翻译", "设置")
    val icons = listOf<ImageVector>(Icons.Filled.Description, Icons.Filled.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = screen) },
                        label = { Text(screen) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            count = items.size,
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            userScrollEnabled = false // Disable swipe gesture
        ) { page ->
            when (page) {
                0 -> TranslatorScreen(
                    viewModel = viewModel,
                    onSelectLanguageGroup = onSelectLanguageGroup,
                    onSave = onSave
                )
                1 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
