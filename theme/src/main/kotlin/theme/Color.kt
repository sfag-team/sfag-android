package com.droidhen.formalautosim.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val blue_one = Color(0xFF5B7065) //primary
val light_gray = Color(0xFFEDFFF5)//background
val medium_gray = Color(0x4F1A030C)
val blue_two = Color(0xFF2E3D3D) //secondary
val blue_three = Color(0xFF052836) //tertiary
val perlamutr_white = Color(0xFFF4FFFF) //surface
val light_blue = Color(0xFFB1E6D1) //primaryContainer
val unable_views = Color(0xFFA0BBB9)

val error_red = Color(0xFF8C230F)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldDefaults.defaultTextInputColor() =  outlinedTextFieldColors(
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.secondary,
    unfocusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
    containerColor = Color.White,
    unfocusedTextColor = MaterialTheme.colorScheme.secondary,
    focusedTextColor = MaterialTheme.colorScheme.primary,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorTextColor = MaterialTheme.colorScheme.secondary,
    errorLabelColor = MaterialTheme.colorScheme.secondary,
    errorCursorColor = MaterialTheme.colorScheme.secondary,
    errorContainerColor = Color.White

)
