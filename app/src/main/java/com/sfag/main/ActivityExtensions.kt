package com.sfag.main

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("SourceLockedOrientationActivity")
fun AppCompatActivity.configureScreenOrientation() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )
    // TODO: Allow rotation on large screens (≥600dp) in the future.
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}
