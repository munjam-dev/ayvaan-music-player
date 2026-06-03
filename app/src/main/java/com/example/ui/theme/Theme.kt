package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    themeMode: String = "Dark Mode",
    accentColorName: String = "Cyber Accent",
    customAccentHex: String = "#8A2BE2",
    dynamicColor: Boolean = false,
    artworkDominantColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = true

    val basePrimary = if (dynamicColor && artworkDominantColor != null) {
        artworkDominantColor
    } else {
        when (accentColorName) {
            "Cyber Accent" -> Color(0xFF8B5CF6)
            "Purple", "Electric Purple" -> Color(0xFF8A2BE2)
            "Blue", "Neon Blue" -> Color(0xFF2563EB)
            "Cyan", "Cyan Glow" -> Color(0xFF00D4FF)
            "Pink", "Pink Glow", "Pink Pulse" -> Color(0xFFD946EF)
            "Green" -> Color(0xFF10B981)
            "Orange" -> Color(0xFFF97316)
            "Custom" -> {
                try {
                    Color(android.graphics.Color.parseColor(customAccentHex))
                } catch (e: Exception) {
                    Color(0xFF8A2BE2)
                }
            }
            else -> Color(0xFF8B5CF6) // Default fallback to Cyber Accent Primary
        }
    }

    val baseSecondary = when (accentColorName) {
        "Cyber Accent" -> Color(0xFF06B6D4)
        else -> if (isDark) RawCyberPink else Color(0xFFEC4899)
    }

    val baseTertiary = when (accentColorName) {
        "Cyber Accent" -> Color(0xFF22D3EE)
        else -> if (isDark) RawCyberCyan else Color(0xFF06B6D4)
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = basePrimary,
            secondary = baseSecondary,
            tertiary = baseTertiary,
            background = PitchBlack,
            surface = LuxuryGrey,
            onPrimary = TextLight,
            onSecondary = TextLight,
            onBackground = TextLight,
            onSurface = TextLight,
            surfaceVariant = CardGrey
        )
    } else {
        lightColorScheme(
            primary = basePrimary,
            secondary = baseSecondary,
            tertiary = baseTertiary,
            background = Color(0xFFF5F5F7),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF0F0F14),
            onSurface = Color(0xFF1E1E24),
            surfaceVariant = Color(0xFFE5E7EB)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            var depth = 0
            while (context is android.content.ContextWrapper && depth < 20) {
                if (context is Activity) break
                context = context.baseContext
                depth++
            }
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = if (isDark) PitchBlack.toArgb() else Color(0xFFF5F5F7).toArgb()
                window.navigationBarColor = if (isDark) PitchBlack.toArgb() else Color(0xFFF5F5F7).toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
