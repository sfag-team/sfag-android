package com.sfag.shared.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.sfag.R

val AppTypography = Typography()

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
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

    val darkScheme = darkColorScheme(
        primary = colorResource(R.color.primary_dark),
        onPrimary = colorResource(R.color.on_primary_dark),
        primaryContainer = colorResource(R.color.primary_container_dark),
        onPrimaryContainer = colorResource(R.color.on_primary_container_dark),
        secondary = colorResource(R.color.secondary_dark),
        onSecondary = colorResource(R.color.on_secondary_dark),
        secondaryContainer = colorResource(R.color.secondary_container_dark),
        onSecondaryContainer = colorResource(R.color.on_secondary_container_dark),
        tertiary = colorResource(R.color.tertiary_dark),
        onTertiary = colorResource(R.color.on_tertiary_dark),
        tertiaryContainer = colorResource(R.color.tertiary_container_dark),
        onTertiaryContainer = colorResource(R.color.on_tertiary_container_dark),
        error = colorResource(R.color.error_dark),
        onError = colorResource(R.color.on_error_dark),
        errorContainer = colorResource(R.color.error_container_dark),
        onErrorContainer = colorResource(R.color.on_error_container_dark),
        background = colorResource(R.color.background_dark),
        onBackground = colorResource(R.color.on_background_dark),
        surface = colorResource(R.color.surface_dark),
        onSurface = colorResource(R.color.on_surface_dark),
        surfaceVariant = colorResource(R.color.surface_variant_dark),
        onSurfaceVariant = colorResource(R.color.on_surface_variant_dark),
        outline = colorResource(R.color.outline_dark),
        outlineVariant = colorResource(R.color.outline_variant_dark),
        scrim = colorResource(R.color.scrim_dark),
        inverseSurface = colorResource(R.color.inverse_surface_dark),
        inverseOnSurface = colorResource(R.color.inverse_on_surface_dark),
        inversePrimary = colorResource(R.color.inverse_primary_dark),
        surfaceDim = colorResource(R.color.surface_dim_dark),
        surfaceBright = colorResource(R.color.surface_bright_dark),
        surfaceContainerLowest = colorResource(R.color.surface_container_lowest_dark),
        surfaceContainerLow = colorResource(R.color.surface_container_low_dark),
        surfaceContainer = colorResource(R.color.surface_container_dark),
        surfaceContainerHigh = colorResource(R.color.surface_container_high_dark),
        surfaceContainerHighest = colorResource(R.color.surface_container_highest_dark),
    )

    val colorScheme = if (darkTheme) darkScheme else lightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
