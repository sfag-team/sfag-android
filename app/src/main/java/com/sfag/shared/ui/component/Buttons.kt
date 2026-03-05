package com.sfag.shared.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DefaultButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Int = 48,
    conditionToEnable: Boolean = true,
    onClick: () -> Unit
) {
    val enabledColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    val disabledColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Button(
        onClick = { if (conditionToEnable) onClick() },
        modifier = modifier
            .widthIn(min = 96.dp)
            .heightIn(min = height.dp),
        shape = MaterialTheme.shapes.medium,
        colors = if (conditionToEnable) enabledColors else disabledColors,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        Text(text = text)
    }
}

/**
 * Square-ish icon button: secondaryContainer background (inactive) or primary (active),
 * medium rounded corners.
 *
 * Pass [modifier] with `Modifier.weight(1f)` when used in a Row that distributes space evenly.
 */
@Composable
fun DefaultIconButton(
    icon: Int,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    height: Dp = 56.dp,
    iconSize: Dp = 32.dp,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
