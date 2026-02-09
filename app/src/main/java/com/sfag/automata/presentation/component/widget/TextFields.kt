package com.sfag.automata.presentation.component.widget

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.shared.presentation.theme.defaultTextInputColor
import com.sfag.shared.presentation.theme.blue_one
import com.sfag.shared.presentation.theme.error_red


@Composable
fun DefaultTextField(
    modifier: Modifier = Modifier,
    hint: String,
    value: String,
    requirementText: String,
    onTextChange: (String) -> Unit,
    isRequirementsComplete: () -> Boolean
) {
    var isFocused by remember {
        mutableStateOf(false)
    }
    val isError = !isRequirementsComplete()
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            onTextChange(text.filterNot { it == '\n' })
        },
        modifier = modifier.onFocusChanged { state ->
            isFocused = state.isFocused
        },
        label = { Text(hint) },
        textStyle = TextStyle(color = blue_one, fontSize = 20.sp),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.defaultTextInputColor(),
        isError = isError
    )

    if (isError && isFocused) {
        Text(
            text = requirementText,
            fontSize = 12.sp,
            color = error_red,
            modifier = Modifier
                .height(31.dp)
                .offset(y = (-4).dp)
                .padding(2.dp)
        )
    }
}
