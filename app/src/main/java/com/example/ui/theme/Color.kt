package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Raw static fallbacks to avoid layout rendering cycles
val RawCyberViolet = Color(0xFF8A2BE2)
val RawCyberPink = Color(0xFFD946EF)
val RawCyberCyan = Color(0xFF00D4FF)

val CyberViolet: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val CyberPink: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

val CyberCyan: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

// Cinematic Luxury Darks remains static
val PitchBlack = Color(0xFF050505)
val LuxuryGrey = Color(0xFF121212)
val CardGrey = Color(0xFF1B1B26)
val TextLight = Color(0xFFF3F2F8)
val TextDim = Color(0xFFA0A0C0)

// Neon Gradients & Cyberpunk Accents
val CyberIndigo = Color(0xFF6C63FF)

// Standard Android Material Scheme fallback entries
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
