package com.sfag.home.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sfag.shared.ui.theme.AppTheme
import com.sfag.shared.ui.activity.configureScreenOrientation
import com.sfag.home.ui.screen.SplashScreen
import com.sfag.home.ui.navigation.HomeDestinations
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureScreenOrientation()

        setContent {
            AppTheme {
                rememberNavController().apply {
                    NavHost(
                        navController = this,
                        startDestination = HomeDestinations.SPLASH.route
                    ) {
                        composable(HomeDestinations.SPLASH.route) {
                            SplashScreen(navigateToMainActivity = ::navigateToMainActivity)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
