package com.sfag.shared.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

@SuppressLint("SourceLockedOrientationActivity")
fun ComponentActivity.configureScreenOrientation() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(
            Color.TRANSPARENT,
            Color.TRANSPARENT
        )
    )
    // TODO: Allow rotation on large screens (≥600dp) in the future.
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}
