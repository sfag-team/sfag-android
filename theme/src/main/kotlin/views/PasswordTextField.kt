package views

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidhen.formalautosim.presentation.theme.blue_one
import com.droidhen.formalautosim.presentation.theme.defaultTextInputColor
import com.droidhen.formalautosim.presentation.theme.error_red
import com.droidhen.theme.R

@Composable
fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordShowed: MutableState<Boolean>,
    isRequirementsComplete: () -> Boolean
) {

    OutlinedTextField(
        value = password,
        onValueChange = { text ->
            onPasswordChange(text.filterNot { it == '\n' })
        },
        label = { Text(stringResource(R.string.password)) },
        visualTransformation = if (isPasswordShowed.value) VisualTransformation.None else PasswordVisualTransformation(),
        textStyle = TextStyle(color = blue_one, fontSize = 20.sp),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.defaultTextInputColor(),
        isError = !isRequirementsComplete(),
        trailingIcon = {
            val image = ImageVector.vectorResource(
                if (isPasswordShowed.value) R.drawable.show_password
                else R.drawable.hide_password
            )

            IconButton(onClick = {
                isPasswordShowed.value = !isPasswordShowed.value
            }) {
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    tint = blue_one,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    )
}

@Composable
fun ConfirmPasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isRequirementsComplete: () -> Boolean,
    passwordVisible: Boolean,
) {
    var isFocused by remember {
        mutableStateOf(false)
    }
    val isError = !isRequirementsComplete()
    OutlinedTextField(
        value = password,
        modifier = Modifier.onFocusChanged { state ->
            isFocused = state.isFocused
        },
        onValueChange = { text ->
            onPasswordChange(text.filterNot { it == '\n' })
        },
        label = { Text(stringResource(R.string.confirm_password)) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        textStyle = TextStyle(color = blue_one, fontSize = 20.sp),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.defaultTextInputColor(),
        isError = isError
    )
    if (isError && isFocused) {
        Text(
            text = stringResource(R.string.password_requirements),
            fontSize = 12.sp,
            color = error_red,
            modifier = Modifier
                .height(44.dp)
                .offset(y = (-4).dp)
        )
    }
}

@Composable
fun DefaultTextField(
    hint:String,
    value: String,
    requirementText:String,
    onTextChange: (String) -> Unit,
    isRequirementsComplete: () -> Boolean,
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
        modifier = Modifier.onFocusChanged { state ->
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