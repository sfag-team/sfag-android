package com.sfag.shared.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class CustomColorScheme(
    val acceptedBackground: Color,
    val dagAnimationHighlight: Color,
    val dagEdge: Color,
    val dagGroupingRect: Color,
    val dagLeafHighlight: Color,
    val dagNodeText: Color,
    val dagNonTerminalNode: Color,
    val dagTerminalNode: Color,
    val derivationCompleted: Color,
    val derivationHighlight: Color,
    val disabledContainer: Color,
    val rejectedBackground: Color,
)

val lightCustomColorScheme = CustomColorScheme(
    acceptedBackground = Color(0xFF38F292),
    dagAnimationHighlight = Color(0xFFF5823B),
    dagEdge = Color.White,
    dagGroupingRect = Color(0xFF7BC2ED),
    dagLeafHighlight = Color.Yellow,
    dagNodeText = Color.White,
    dagNonTerminalNode = Color(0xFF77E681),
    dagTerminalNode = Color(0xFF0479C2),
    derivationCompleted = Color.Yellow,
    derivationHighlight = Color(0xFF2779C2),
    disabledContainer = Color(0xFFA0BBB9),
    rejectedBackground = Color(0xFFDB5C65),
)

val LocalCustomColorScheme = compositionLocalOf { lightCustomColorScheme }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.customColorScheme: CustomColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalCustomColorScheme.current
