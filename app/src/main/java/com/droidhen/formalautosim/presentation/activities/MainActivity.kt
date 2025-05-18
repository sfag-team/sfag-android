package com.droidhen.formalautosim.presentation.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidhen.formalautosim.presentation.navigation.AppDestinations
import com.droidhen.formalautosim.presentation.navigation.screens.ExamplesScreen
import com.droidhen.formalautosim.presentation.navigation.screens.MainScreen
import com.droidhen.formalautosim.presentation.theme.FormalAutoSimTheme
import com.droidhen.formalautosim.presentation.views.BottomBar
import com.droidhen.formalautosim.utils.extensions.SetDefaultSettings
import com.example.gramatika.GrammarActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var hideStatusBar = mutableIntStateOf(0)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FormalAutoSimTheme {
                SetDefaultSettings()
                key(hideStatusBar) {
                    hideSystemBars()
                }
                rememberNavController().apply {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        NavHost(
                            navController = this@apply,
                            startDestination = AppDestinations.MAIN.route,
                            modifier = Modifier.weight(9f)
                        ) {
                            composable(route = AppDestinations.MAIN.route) {
                                MainScreen(navToGrammar = {navToGrammarActivity(null)}, navToAutomata = {navToAutomataActivity(null)}, navToExamplesScreen = {
                                    navigate(AppDestinations.EXAMPLES.route)
                                })
                            }
                            composable(route = AppDestinations.EXAMPLES.route){
                                ExamplesScreen(navBack = {
                                    navigate(AppDestinations.MAIN.route)
                                },
                                    navToGrammar = { grammarUri ->
                                        navToGrammarActivity(grammarUri)
                                    }, navToAutomata = { automataUri ->
                                        navToAutomataActivity(automataUri)
                                    })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideStatusBar.intValue += 1
    }

    @SuppressLint("ComposableNaming")
    @Composable
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, LocalView.current).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun navToGrammarActivity(uri: String?){
        val intent = Intent(this, GrammarActivity::class.java)
        uri?.apply { intent.putExtra("example uri", this) }
        startActivity(intent)
    }

    private fun navToAutomataActivity(uri: String?){
        val intent = Intent(this, AutomataActivity::class.java)
        uri?.apply { intent.putExtra("example uri", this) }
        startActivity(intent)
    }
}