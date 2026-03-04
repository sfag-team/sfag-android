package com.sfag.automata.ui.component.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

@Composable
fun DefaultTextField(
    modifier: Modifier = Modifier,
    hint: String,
    value: String,
    requirementText: String = "",
    suffix: String = "",
    labelColor: Color? = null,
    textColor: Color? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onTextChange: (String) -> Unit,
    isRequirementsComplete: (() -> Boolean)? = null
) {
    val filteredChange = { text: String -> onTextChange(text.filterNot { it == '\n' }.trim().lowercase()) }
    var isFocused by remember { mutableStateOf(false) }
    val isError = isRequirementsComplete?.let { !it() } ?: false
    val resolvedTextColor = textColor ?: MaterialTheme.colorScheme.onSurface
    val suffixColor = MaterialTheme.colorScheme.outline

    TextField(
        value = value,
        onValueChange = filteredChange,
        modifier = modifier.onFocusChanged { state -> isFocused = state.isFocused },
        label = if (hint.isNotEmpty()) {{ Text(hint) }} else null,
        singleLine = true,
        visualTransformation = if (suffix.isNotEmpty()) SuffixVisualTransformation(suffix, suffixColor) else VisualTransformation.None,
        textStyle = MaterialTheme.typography.titleLarge.copy(color = resolvedTextColor),
        shape = MaterialTheme.shapes.extraSmall,
        trailingIcon = trailingIcon,
        supportingText = if (isError && isFocused && requirementText.isNotEmpty()) {
            { Text(text = requirementText) }
        } else null,
        colors = TextFieldDefaults.colors(
            cursorColor = MaterialTheme.colorScheme.primary,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            errorCursorColor = MaterialTheme.colorScheme.primary,
            errorIndicatorColor = Color.Transparent,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorTextColor = resolvedTextColor,
            errorSupportingTextColor = MaterialTheme.colorScheme.error,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            focusedLabelColor = labelColor ?: MaterialTheme.colorScheme.primary,
            focusedTextColor = resolvedTextColor,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedLabelColor = labelColor ?: MaterialTheme.colorScheme.outline,
            unfocusedTextColor = resolvedTextColor,
        ),
        isError = isError
    )
}

private class SuffixVisualTransformation(private val suffix: String, private val suffixColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformedText = buildAnnotatedString {
            append(text)
            withStyle(style = SpanStyle(color = suffixColor)) {
                append(suffix)
            }
        }
        return TransformedText(
            text = transformedText,
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset
                override fun transformedToOriginal(offset: Int): Int =
                    offset.coerceAtMost(text.length)
            }
        )
    }
}
