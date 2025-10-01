package com.sfag.automata.theme

import androidx.compose.runtime.Composable
import com.sfag.grammar.theme.GrammarTheme

@Composable
fun Theme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    GrammarTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
