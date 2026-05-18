package com.sfag.main.ui.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DefaultButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
) {
    val enabledColors =
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    val disabledColors =
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    Button(
        onClick = { if (enabled) onClick() },
        modifier = modifier.widthIn(min = 96.dp).heightIn(min = height),
        shape = MaterialTheme.shapes.medium,
        colors = if (enabled) enabledColors else disabledColors,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = text)
    }
}

/**
 * Square-ish icon button: secondaryContainer background (inactive) or primary (active), medium
 * rounded corners.
 *
 * Pass [modifier] with `Modifier.weight(1f)` when used in a Row that distributes space evenly.
 */
@Composable
fun DefaultIconButton(
    onClick: () -> Unit,
    icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentDescription: String = "",
    height: Dp = 56.dp,
    iconSize: Dp = 32.dp,
) {
    DefaultIconButtonBox(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        height = height,
    ) { tint ->
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}

/** Same as [DefaultIconButton] but takes a Material [ImageVector] (e.g. `Icons.Outlined.Edit`). */
@Composable
fun DefaultIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentDescription: String = "",
    height: Dp = 56.dp,
    iconSize: Dp = 32.dp,
) {
    DefaultIconButtonBox(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        height = height,
    ) { tint ->
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}

@Composable
private fun DefaultIconButtonBox(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    selected: Boolean,
    height: Dp,
    content: @Composable (tint: Color) -> Unit,
) {
    Box(
        modifier =
            modifier
                .height(height)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSecondaryContainer
        content(tint)
    }
}
