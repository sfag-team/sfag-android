package com.sfag.shared.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.sfag.shared.theme.unable_views

@Composable
fun DefaultButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Int = 48,
    conditionToEnable: Boolean = true,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "button animation",
    )

    val enabledColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    val disabledColors = ButtonDefaults.buttonColors(
        containerColor = unable_views,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )

    Button(
        onClick = {
            if (conditionToEnable) {
                isPressed = true
                onClick()
                isPressed = false
            }
        },
        modifier = modifier
            .padding(1.dp)
            .width(95.dp)
            .height(height.dp)
            .scale(scale)
            .border(
                2.dp,
                shape = RoundedCornerShape(10.dp),
                color = if (conditionToEnable)
                    MaterialTheme.colorScheme.secondary
                else
                    unable_views
            )
            .shadow(
                3.dp,
                RoundedCornerShape(10.dp),
                clip = false,
                ambientColor = MaterialTheme.colorScheme.secondary
            ),
        shape = RoundedCornerShape(10.dp),
        colors = if (conditionToEnable) enabledColors else disabledColors,
        contentPadding = PaddingValues(
            horizontal = 8.dp,
            vertical = 4.dp
        )
    ) {
        Text(text = text)
    }
}
