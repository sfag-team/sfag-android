package com.sfag.shared.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sfag.R

@Composable
fun DefaultDialogWindow(
    title: String?,
    modifier: Modifier = Modifier,
    confirmLabel: String = stringResource(R.string.save),
    dismissLabel: String = stringResource(R.string.discard),
    conditionToEnable: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = conditionToEnable,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
        title = title?.let { { Text(it, style = MaterialTheme.typography.headlineSmall) } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
        },
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}
