package com.example.pdtranslator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.pdtranslator.ThemeColor

private val DarkColorPalette = darkColorScheme(
  primary = Purple200,
  secondary = Teal200,
  background = Color(0xFF121212),
  surface = Color(0xFF1E1E1E),
  onPrimary = Color.Black,
  onSecondary = Color.Black,
  onBackground = Color.White,
  onSurface = Color.White,
)

private val LightColorPalette = lightColorScheme(
  primary = Purple500,
  secondary = Teal200,
  background = Color.White,
  surface = Color(0xFFF5F5F5),
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onBackground = Color.Black,
  onSurface = Color.Black,
)

@Composable
fun PDTranslatorTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeColor: ThemeColor = ThemeColor.DEFAULT,
  content: @Composable () -> Unit
) {
  val colorScheme = when (themeColor) {
    // DEFAULT and M3: use dynamic color on Android 12+, else fallback
    ThemeColor.DEFAULT -> {
      if (darkTheme) DarkColorPalette else LightColorPalette
    }
    ThemeColor.M3 -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else {
        if (darkTheme) DarkColorPalette else LightColorPalette
      }
    }
    // Custom themes: always use their defined palette
    ThemeColor.GREEN -> {
      if (darkTheme) GreenThemeColors.darkColorScheme else GreenThemeColors.lightColorScheme
    }
    ThemeColor.LAVENDER -> {
      if (darkTheme) LavenderThemeColors.darkColorScheme else LavenderThemeColors.lightColorScheme
    }
    ThemeColor.MODERN -> {
      if (darkTheme) ModernThemeColors.darkColorScheme else ModernThemeColors.lightColorScheme
    }
    ThemeColor.PIXEL_DUNGEON -> {
      if (darkTheme) PixelDungeonThemeColors.darkColorScheme else PixelDungeonThemeColors.lightColorScheme
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = Shapes(),
    content = content
  )
}
