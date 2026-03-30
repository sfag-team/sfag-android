package com.sfag.main.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import com.sfag.R

@OptIn(ExperimentalTextApi::class)
val RobotoFlexFamily = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        style = FontStyle.Normal
    ),
    Font(
        resId = R.font.roboto_flex,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.slant(-12f)
        )
    )
)

val AppTypography = Typography().let { d ->
    Typography(
        displayLarge = d.displayLarge.copy(fontFamily = RobotoFlexFamily),
        displayMedium = d.displayMedium.copy(fontFamily = RobotoFlexFamily),
        displaySmall = d.displaySmall.copy(fontFamily = RobotoFlexFamily),
        headlineLarge = d.headlineLarge.copy(fontFamily = RobotoFlexFamily),
        headlineMedium = d.headlineMedium.copy(fontFamily = RobotoFlexFamily),
        headlineSmall = d.headlineSmall.copy(fontFamily = RobotoFlexFamily),
        titleLarge = d.titleLarge.copy(fontFamily = RobotoFlexFamily),
        titleMedium = d.titleMedium.copy(fontFamily = RobotoFlexFamily),
        titleSmall = d.titleSmall.copy(fontFamily = RobotoFlexFamily),
        bodyLarge = d.bodyLarge.copy(fontFamily = RobotoFlexFamily),
        bodyMedium = d.bodyMedium.copy(fontFamily = RobotoFlexFamily),
        bodySmall = d.bodySmall.copy(fontFamily = RobotoFlexFamily),
        labelLarge = d.labelLarge.copy(fontFamily = RobotoFlexFamily),
        labelMedium = d.labelMedium.copy(fontFamily = RobotoFlexFamily),
        labelSmall = d.labelSmall.copy(fontFamily = RobotoFlexFamily),
    )
}
