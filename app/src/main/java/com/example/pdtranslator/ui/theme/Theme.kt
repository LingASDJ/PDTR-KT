package com.example.pdtranslator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdtranslator.R
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

// ── Pixel Dungeon: chiseled stone shapes ──
private val PixelDungeonShapes = Shapes(
  extraSmall = CutCornerShape(3.dp),
  small      = CutCornerShape(6.dp),
  medium     = CutCornerShape(8.dp),
  large      = CutCornerShape(10.dp),
  extraLarge = CutCornerShape(12.dp)
)

// ── Pixel Dungeon fonts ──
// fusion_pixel.ttf — from MLPD, supports CJK + Latin, true pixel bitmap feel
// pixel_font.ttf  — from MLPD, Latin-only pixel font
val PixelFontFamily = FontFamily(
  Font(R.font.fusion_pixel, FontWeight.Normal),
  Font(R.font.fusion_pixel, FontWeight.Medium),
  Font(R.font.fusion_pixel, FontWeight.SemiBold),
  Font(R.font.fusion_pixel, FontWeight.Bold),
  Font(R.font.pixel_font, FontWeight.Light)
)

private val PixelDungeonTypography = Typography(
  displayLarge  = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Bold,     fontSize = 52.sp, letterSpacing = 1.5.sp),
  displayMedium = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Bold,     fontSize = 40.sp, letterSpacing = 1.5.sp),
  displaySmall  = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Bold,     fontSize = 32.sp, letterSpacing = 1.5.sp),
  headlineLarge = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Bold,     fontSize = 28.sp, letterSpacing = 1.sp),
  headlineMedium= TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Bold,     fontSize = 24.sp, letterSpacing = 1.sp),
  headlineSmall = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 1.sp),
  titleLarge    = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 1.2.sp),
  titleMedium   = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.2.sp),
  titleSmall    = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 1.sp),
  bodyLarge     = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
  bodyMedium    = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.5.sp),
  bodySmall     = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.Normal,   fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.5.sp),
  labelLarge    = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.sp),
  labelMedium   = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp),
  labelSmall    = TextStyle(fontFamily = PixelFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.sp),
)

@Composable
fun PDTranslatorTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeColor: ThemeColor = ThemeColor.DEFAULT,
  content: @Composable () -> Unit
) {
  val isPixelDungeon = themeColor == ThemeColor.PIXEL_DUNGEON

  val colorScheme = when (themeColor) {
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
      // Always dark — the dungeon has no daylight
      // rememberTimeTick() makes this recompose every 60s so colors shift
      val tick = rememberTimeTick()
      val tint = currentTimeTint()
      val base = PixelDungeonThemeColors.darkColorScheme
      base.copy(
        // Strong zone tinting — visible color shift every 4 hours
        primary = lerp(base.primary, tint.accent2, 0.35f),
        secondary = lerp(base.secondary, tint.accent1, 0.4f),
        secondaryContainer = lerp(base.secondaryContainer, tint.surfaceTint, 0.4f),
        tertiary = lerp(base.tertiary, tint.accent1, 0.6f),
        tertiaryContainer = lerp(base.tertiaryContainer, tint.surfaceTint, 0.5f),
        outline = lerp(base.outline, tint.accent1, 0.35f),
        outlineVariant = lerp(base.outlineVariant, tint.accent1, 0.3f),
        primaryContainer = lerp(base.primaryContainer, tint.accent1, 0.25f),
        onPrimaryContainer = lerp(base.onPrimaryContainer, tint.accent2, 0.2f),
        // Also tint background/surface so standalone screens show zone color
        background = lerp(base.background, tint.surfaceTint, 0.5f),
        surface = lerp(base.surface, tint.surfaceTint, 0.3f),
        surfaceVariant = lerp(base.surfaceVariant, tint.surfaceTint, 0.3f),
      )
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = if (isPixelDungeon) PixelDungeonTypography else Typography,
    shapes = if (isPixelDungeon) PixelDungeonShapes else Shapes(),
    content = content
  )
}
