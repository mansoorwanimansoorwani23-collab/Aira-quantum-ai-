package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AiraDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF030705),
    primaryContainer = Color(0xFF0C2B19),
    onPrimaryContainer = Color(0xFFE6FFF0),
    secondary = QuantumCyan,
    onSecondary = Color(0xFF030705),
    secondaryContainer = Color(0xFF09291E),
    onSecondaryContainer = Color(0xFFD1FFE9),
    tertiary = LaserLime,
    onTertiary = Color(0xFF030705),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = CrimsonRed,
    onError = Color.White
)

private val AiraLightColorScheme = AiraDarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AiraDarkColorScheme,
        typography = Typography,
        content = content
    )
}

