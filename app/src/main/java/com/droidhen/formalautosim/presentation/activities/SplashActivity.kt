package com.droidhen.formalautosim.presentation.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidhen.formalautosim.presentation.navigation.Destinations
import com.droidhen.formalautosim.presentation.navigation.screens.SignInScreen
import com.droidhen.formalautosim.presentation.navigation.screens.SplashScreen
import com.droidhen.formalautosim.presentation.theme.FormalAutoSimTheme
import com.droidhen.formalautosim.utils.extensions.SetDefaultSettings
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
                            LaunchedEffect(Unit) {
                                navigateToMainActivity()
                            }
                                // SignInScreen(::navigateToMainActivity)
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


