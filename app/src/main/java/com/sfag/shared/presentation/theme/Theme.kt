package com.sfag.shared.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF6C8176),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB1E6D1),
        secondary = Color(0xFF2E3D3D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF7FC7A7),
        tertiary = Color(0xFF052836),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFEDFFF5),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFF4FFFF),
        onSurface = Color(0xFF1C1B1F),
        error = Color(0xFF8C230F),
        errorContainer = Color(0xFFEC5C49),
        surfaceContainer = Color(0xFFB1E6D1),
        surfaceContainerHigh = Color(0xFFB1E6D1),
        surfaceContainerLow = Color(0xFFB1E6D1),
    )

    CompositionLocalProvider(LocalCustomColorScheme provides lightCustomColorScheme) {
        MaterialTheme(
            colorScheme = lightColorScheme,
            typography = Typography(),
            content = content
        )
    }
}
