package com.example.pdtranslator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.pdtranslator.ThemeColor

private val DarkColorPalette = darkColorScheme(
    primary = Purple200,
    secondary = Teal200,
    background = Color(0xFF121212), // Standard dark background
    surface = Color(0xFF1E1E1E), // Slightly lighter surface
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorPalette = lightColorScheme(
    primary = Purple500,
    secondary = Teal200,
    background = Color.White,
    surface = Color(0xFFF5F5F5), // A slightly off-white surface
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun PDTranslatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Dynamic color is available on Android 12+
    themeColor: ThemeColor = ThemeColor.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> when (themeColor) {
            ThemeColor.DEFAULT -> DarkColorPalette
            ThemeColor.M3 -> DarkColorPalette // Using default for M3 dark for now
            ThemeColor.GREEN -> GreenThemeColors.darkColorScheme
            ThemeColor.LAVENDER -> LavenderThemeColors.darkColorScheme
        }
        else -> when (themeColor) {
            ThemeColor.DEFAULT -> LightColorPalette
            ThemeColor.M3 -> LightColorPalette // Using default for M3 light for now
            ThemeColor.GREEN -> GreenThemeColors.lightColorScheme
            ThemeColor.LAVENDER -> LavenderThemeColors.lightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}
