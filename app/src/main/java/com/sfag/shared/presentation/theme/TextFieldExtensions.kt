package com.sfag.shared.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun TextFieldDefaults.defaultTextInputColor() = colors(
    cursorColor = MaterialTheme.colorScheme.primary,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    errorCursorColor = MaterialTheme.colorScheme.secondary,
    errorIndicatorColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.secondary,
    errorTextColor = MaterialTheme.colorScheme.secondary,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
    focusedLabelColor = MaterialTheme.colorScheme.secondary,
    focusedTextColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedTextColor = MaterialTheme.colorScheme.secondary,
)
