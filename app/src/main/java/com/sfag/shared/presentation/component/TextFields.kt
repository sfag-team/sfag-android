package com.sfag.shared.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

@Composable
fun ImmutableTextField(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.tertiary,
    fontSize: TextUnit = 24.sp
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = fontSize),
            textAlign = TextAlign.Center,
            color = textColor,
        )
    }
}
