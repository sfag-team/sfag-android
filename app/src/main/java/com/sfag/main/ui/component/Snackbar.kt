package com.sfag.main.ui.component

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable

@Composable
fun DefaultSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState)
}
