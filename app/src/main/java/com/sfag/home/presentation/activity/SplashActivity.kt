package com.sfag.home.presentation.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sfag.shared.presentation.theme.AppTheme
import com.sfag.shared.presentation.activity.configureScreenOrientation
import com.sfag.home.presentation.screen.SplashScreen
import com.sfag.home.presentation.navigation.HomeDestinations
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    private var intentToMainActivity: Intent? = null

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
                            SplashScreen(lifecycleScope, navigateToMainActivity = ::navigateToMainActivity)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        if (intentToMainActivity == null) intentToMainActivity =
            Intent(this, HomeActivity::class.java)
        startActivity(intentToMainActivity)
        finish()
    }
}
