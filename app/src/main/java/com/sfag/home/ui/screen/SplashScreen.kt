package com.sfag.home.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navigateToMainActivity: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1000)
        navigateToMainActivity()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.splash),
            modifier = Modifier.size(300.dp),
            contentDescription = ""
        )
    }
}
