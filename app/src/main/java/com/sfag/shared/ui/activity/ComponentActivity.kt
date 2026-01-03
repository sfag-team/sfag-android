package com.sfag.shared.ui.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

@SuppressLint("SourceLockedOrientationActivity", "WrongConstant")
@Suppress("DEPRECATION")
@Composable
fun ComponentActivity.SetDefaultSettings() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.Companion.Transparent.toArgb()
    window.navigationBarColor = Color.Companion.Transparent.toArgb()
    window.navigationBarDividerColor = Color(0xFF2F3F3F).toArgb()
}