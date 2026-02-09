package com.sfag.shared.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.sfag.R

val AppTypography = Typography()

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val lightScheme = lightColorScheme(
        primary = colorResource(R.color.blue_one),
        secondary = colorResource(R.color.blue_two),
        tertiary = colorResource(R.color.blue_three),
        background = colorResource(R.color.light_gray),
        surface = colorResource(R.color.perlamutr_white),
        primaryContainer = colorResource(R.color.light_blue),
        errorContainer = colorResource(R.color.error_red_light),
        secondaryContainer = colorResource(R.color.darker_light_blue),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        surfaceContainer = colorResource(R.color.light_blue),
        surfaceContainerHigh = colorResource(R.color.light_blue),
        surfaceContainerLow = colorResource(R.color.light_blue),
        onBackground = colorResource(R.color.on_background),
        onSurface = colorResource(R.color.on_surface),
        error = colorResource(R.color.error_red),
    )

    MaterialTheme(
        colorScheme = lightScheme,
        typography = AppTypography,
        content = content
    )
}
