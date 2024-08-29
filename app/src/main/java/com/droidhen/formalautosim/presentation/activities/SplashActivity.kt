package com.droidhen.formalautosim.presentation.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidhen.formalautosim.presentation.navigation.Destinations
import com.droidhen.formalautosim.presentation.navigation.screens.SignInScreen
import com.droidhen.formalautosim.presentation.navigation.screens.SplashScreen
import com.droidhen.formalautosim.presentation.theme.FormalAutoSimTheme
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    private var intentToMainActivity: Intent? = null


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FormalAutoSimTheme {
                SetDefaultSettings()
                rememberNavController().apply {
                    NavHost(
                        navController = this,
                        startDestination = Destinations.SPLASH.route
                    ) {
                        composable(Destinations.SPLASH.route) {
                            SplashScreen(navigateToNextScreen = {
                                navigate(Destinations.SIGN_IN.route)
                            }, ::navigateToMainActivity)
                        }
                        composable(Destinations.SIGN_IN.route) {
                            //navigateToMainActivity()
                            SignInScreen(::navigateToMainActivity)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        if (intentToMainActivity == null) intentToMainActivity =
            Intent(this, MainActivity::class.java)
        startActivity(intentToMainActivity)
        finish()
    }
}

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


