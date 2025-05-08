package com.droidhen.formalautosim.utils.extensions

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("SourceLockedOrientationActivity", "WrongConstant")
@Composable
fun ComponentActivity.SetDefaultSettings() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.Transparent.toArgb()
    window.navigationBarColor = Color.Transparent.toArgb()
    window.navigationBarDividerColor = Color(0xFF2F3F3F).toArgb()
}