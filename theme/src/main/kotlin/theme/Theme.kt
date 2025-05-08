package com.droidhen.formalautosim.presentation.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = blue_one,
    secondary = blue_two,
    tertiary = blue_three,
    background = light_gray,
    surface = perlamutr_white,
)

private val LightColorScheme = lightColorScheme(
    primary = blue_one,
    secondary = blue_two,
    tertiary = blue_three,
    background = light_gray,
    surface = perlamutr_white,
    primaryContainer = light_blue
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),

)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FormalAutoSimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme


        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
}