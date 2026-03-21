package com.example.pdtranslator.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Default ---
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)

// --- Green Theme ---
object GreenThemeColors {
  val lightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF81C784),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEDC8),
    onSecondaryContainer = Color(0xFF33691E),
    tertiary = Color(0xFF009688),
    tertiaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFFF1F8E9),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFE8F5E9),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFDCEDC8),
    onSurfaceVariant = Color(0xFF424242),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFCDD2),
  )
  val darkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF003300),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFDCEDC8),
    tertiary = Color(0xFF4DB6AC),
    tertiaryContainer = Color(0xFF00695C),
    background = Color(0xFF1B1B1B),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF3E3E3E),
    onSurfaceVariant = Color(0xFFC8E6C9),
    error = Color(0xFFEF5350),
    errorContainer = Color(0xFF5D0000),
  )
}

// --- Lavender Theme ---
object LavenderThemeColors {
  val lightColorScheme = lightColorScheme(
    primary = Color(0xFF9575CD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFFB39DDB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DAEF),
    onSecondaryContainer = Color(0xFF4A148C),
    tertiary = Color(0xFFCE93D8),
    tertiaryContainer = Color(0xFFF3E5F5),
    background = Color(0xFFF3E5F5),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFEDE7F6),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE8DAEF),
    onSurfaceVariant = Color(0xFF4A4A4A),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFCDD2),
  )
  val darkColorScheme = darkColorScheme(
    primary = Color(0xFFB388FF),
    onPrimary = Color(0xFF1A0033),
    primaryContainer = Color(0xFF6A1B9A),
    onPrimaryContainer = Color(0xFFE1BEE7),
    secondary = Color(0xFFD1C4E9),
    onSecondary = Color(0xFF1A0033),
    secondaryContainer = Color(0xFF7B1FA2),
    onSecondaryContainer = Color(0xFFE8DAEF),
    tertiary = Color(0xFFCE93D8),
    tertiaryContainer = Color(0xFF6A1B9A),
    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFEDE7F6),
    surface = Color(0xFF2A2A4D),
    onSurface = Color(0xFFEDE7F6),
    surfaceVariant = Color(0xFF3A3A5C),
    onSurfaceVariant = Color(0xFFD1C4E9),
    error = Color(0xFFEF5350),
    errorContainer = Color(0xFF5D0000),
  )
}

// --- Modern Theme (Blue-Indigo, clean & contemporary) ---
object ModernThemeColors {
  val lightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = Color(0xFFFF6F00),
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFFE65100),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF546E7A),
    outline = Color(0xFFB0BEC5),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
  )
  val darkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0A1929),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF0A1929),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFB74D),
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF0A1929),
    onBackground = Color(0xFFE3F2FD),
    surface = Color(0xFF132F4C),
    onSurface = Color(0xFFE3F2FD),
    surfaceVariant = Color(0xFF1E3A5F),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF546E7A),
    error = Color(0xFFEF5350),
    errorContainer = Color(0xFF5D0000),
    onErrorContainer = Color(0xFFFFCDD2),
  )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Pixel Dungeon Theme — Magic Ling Pixel Dungeon style
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Source: Window.java, Toolbar.java, CharSprite.java, StatusPane.java, FogOfWar.java
//
//   MLPD_COLOR / TITLE_COLOR  = 0x00FFFF  cyan        ← signature accent
//   SPD  TITLE_COLOR           = 0xFFFF44  gold-yellow ← title / primary
//   SHPX_COLOR                 = 0x33BB33  green       ← health / positive
//   TEXT_WIN                   = 0xFFFF88 / 0xB2B25F   ← gold text gradient
//   Toolbar BGCOLOR            = 0x7B8073              ← stone grey-green
//   Toolbar arrow              = 0x3D2E18              ← deep brown
//   StatusPane exp             = 0xFFFFAA              ← pale gold
//   CharSprite WARNING         = 0xFF8800              ← torch fire orange
//   HealthBar HP / BG          = 0x00EE00 / 0xCC0000   ← green / red
//   FogOfWar mapped            = 0x193366              ← deep navy
//   Sewers color1/2            = 0x48763C / 0x59994A   ← dungeon moss
//   Halls  color1/2            = 0x801500 / 0xA68521   ← lava + gold

object PixelDungeonThemeColors {

  // ── Light: Sepia parchment / dungeon map ──
  // (kept as a safe fallback, but PD should always force dark)
  val lightColorScheme = lightColorScheme(
    primary = Color(0xFF6B5B2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADBA5),
    onPrimaryContainer = Color(0xFF3D2E18),
    secondary = Color(0xFF48763C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE0C4),
    onSecondaryContainer = Color(0xFF1B4332),
    tertiary = Color(0xFF801500),
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF660000),
    background = Color(0xFFF0E6D2),
    onBackground = Color(0xFF2D1F14),
    surface = Color(0xFFE8DABE),
    onSurface = Color(0xFF2D1F14),
    surfaceVariant = Color(0xFFD8CCB0),
    onSurfaceVariant = Color(0xFF5D4E37),
    outline = Color(0xFF7B8073),
    error = Color(0xFFCC0000),
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF660000),
  )

  // ── Dark: THE dungeon experience ──
  // Deep cave lit by torches (orange) and magical energy (cyan).
  // Gold text floats over near-black stone.
  val darkColorScheme = darkColorScheme(
    // PRIMARY = Gold — titles, buttons, active indicators
    primary            = Color(0xFFFFFF44),  // SPD TITLE_COLOR — blazing gold
    onPrimary          = Color(0xFF1C1200),  // near-black on gold buttons
    primaryContainer   = Color(0xFF3D2E18),  // Toolbar brown — stone button bg
    onPrimaryContainer = Color(0xFFFFFF88),  // TEXT_WIN bright gold

    // SECONDARY = Cyan — the MLPD signature magic glow
    secondary            = Color(0xFF00FFFF),  // MLPD_COLOR — pure cyan
    onSecondary          = Color(0xFF001A1A),
    secondaryContainer   = Color(0xFF0A2626),  // Deep dark teal cave
    onSecondaryContainer = Color(0xFF66FFFF),  // Brighter cyan

    // TERTIARY = Torch fire orange — warnings, accents
    tertiary            = Color(0xFFFF8800),  // CharSprite WARNING
    tertiaryContainer   = Color(0xFF3D1E00),  // Dark ember
    onTertiaryContainer = Color(0xFFFFBB44),  // Warm flame

    // SURFACES — dark stone, opaque for standalone screens, semi-transparent for cards
    background     = Color(0xFF080604),  // Very dark stone (fallback for screens without brick bg)
    onBackground   = Color(0xFFFFFFAA),  // StatusPane exp text — pale gold readability
    surface        = Color(0xCC12100C),  // Dark stone floor with alpha (cards over brick)
    onSurface      = Color(0xFFE0D4B8),  // Parchment white — main text
    surfaceVariant = Color(0xCC1E1810),  // Slightly lighter stone with alpha (input fields)
    onSurfaceVariant = Color(0xFFB2B25F), // TEXT_WIN[1] — muted gold secondary text

    // OUTLINES — weathered stone edges
    outline        = Color(0xFF4A4038),  // Visible stone border
    outlineVariant = Color(0xFF2E2418),  // Subtle separator

    // ERROR — Demon Halls lava/blood
    error            = Color(0xFFFF0000),  // CharSprite NEGATIVE — pure red
    onError          = Color(0xFF1C0000),
    errorContainer   = Color(0xFF4A0000),  // Deep blood
    onErrorContainer = Color(0xFFFF6666),  // Lighter red text

    // INVERSE — for snackbars etc
    inverseSurface   = Color(0xFFE0D4B8),
    inverseOnSurface = Color(0xFF12100C),
    inversePrimary   = Color(0xFF8D6E1F),
    scrim            = Color(0xFF000000),
  )
}
