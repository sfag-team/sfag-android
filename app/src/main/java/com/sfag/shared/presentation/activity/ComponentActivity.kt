package com.sfag.shared.presentation.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

@SuppressLint("SourceLockedOrientationActivity")
fun ComponentActivity.configureScreenOrientation() {
    enableEdgeToEdge()
    // TODO: Allow rotation on large screens (≥600dp) in the future.
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}
