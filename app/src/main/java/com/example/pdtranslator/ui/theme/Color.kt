package com.example.pdtranslator.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)

// Custom Theme Definitions

object GreenThemeColors {
    val light_primary = Color(0xFF4CAF50) // A vibrant green
    val light_secondary = Color(0xFF81C784) // A lighter shade of green
    val light_background = Color(0xFFF1F8E9) // A very light green, almost white
    val light_surface = Color(0xFFE8F5E9) // A light green surface

    val dark_primary = Color(0xFF66BB6A) // A softer green for dark mode
    val dark_secondary = Color(0xFFA5D6A7) // A lighter green for accents
    val dark_background = Color(0xFF1B1B1B) // A darker, slightly green-tinted background
    val dark_surface = Color(0xFF2C2C2C) // A dark grey surface

    val lightColorScheme = lightColorScheme(
        primary = light_primary,
        secondary = light_secondary,
        background = light_background,
        surface = light_surface,
    )

    val darkColorScheme = darkColorScheme(
        primary = dark_primary,
        secondary = dark_secondary,
        background = dark_background,
        surface = dark_surface,
    )
}

object LavenderThemeColors {
    val light_primary = Color(0xFF9575CD) // A pleasant lavender
    val light_secondary = Color(0xFFB39DDB) // A lighter lavender
    val light_background = Color(0xFFF3E5F5) // A very light purple background
    val light_surface = Color(0xFFEDE7F6) // A light purple surface

    val dark_primary = Color(0xFFB388FF) // A vibrant lavender for dark mode
    val dark_secondary = Color(0xFFD1C4E9) // A light lavender for accents
    val dark_background = Color(0xFF1A1A2E) // A deep blue-purple background
    val dark_surface = Color(0xFF2A2A4D) // A dark purple-blue surface

    val lightColorScheme = lightColorScheme(
        primary = light_primary,
        secondary = light_secondary,
        background = light_background,
        surface = light_surface,
    )

    val darkColorScheme = darkColorScheme(
        primary = dark_primary,
        secondary = dark_secondary,
        background = dark_background,
        surface = dark_surface,
    )
}
