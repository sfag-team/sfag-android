package com.droidhen.formalautosim.presentation.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidhen.formalautosim.presentation.navigation.Destinations
import com.droidhen.formalautosim.presentation.navigation.screens.SignInScreen
import com.droidhen.formalautosim.presentation.navigation.screens.SignUpScreen
import com.droidhen.formalautosim.presentation.navigation.screens.SplashScreen

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    private var intentToMainActivity: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDefaultSettings()
        setContent {
            rememberNavController().apply {
                NavHost(navController = this, startDestination = Destinations.SPLASH.route) {
                    composable(Destinations.SPLASH.route) {
                        SplashScreen(navigateToNextScreen = {
                            navigate(Destinations.SIGN_IN.route)
                        }, ::navigateToMainActivity)
                    }
                    composable(Destinations.SIGN_IN.route) {
                        SignInScreen(navigateToSignUpScreen = {
                            navigate(Destinations.SIGN_UP.route)
                        }, ::navigateToMainActivity)
                    }
                    composable(Destinations.SIGN_UP.route) {
                        SignUpScreen(navigateToSignInScreen = {
                            navigate(Destinations.SIGN_IN.route)
                        }, ::navigateToMainActivity)
                    }
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        if(intentToMainActivity==null) intentToMainActivity = Intent(this, MainActivity::class.java)
        startActivity(intentToMainActivity)
        finish()
    }
}

@SuppressLint("SourceLockedOrientationActivity")
fun ComponentActivity.setDefaultSettings(){
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    enableEdgeToEdge()
}


