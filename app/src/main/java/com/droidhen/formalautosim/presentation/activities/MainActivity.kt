package com.droidhen.formalautosim.presentation.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidhen.formalautosim.presentation.navigation.AppDestinations
import com.droidhen.formalautosim.presentation.navigation.screens.AboutScreen
import com.droidhen.formalautosim.presentation.navigation.screens.ExamplesScreen
import com.droidhen.formalautosim.presentation.navigation.screens.MainScreen
import com.droidhen.formalautosim.presentation.theme.FormalAutoSimTheme
import com.droidhen.formalautosim.utils.extensions.SetDefaultSettings
import com.example.gramatika.GrammarActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FormalAutoSimTheme {
                SetDefaultSettings()

                rememberNavController().apply {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        NavHost(
                            navController = this@apply,
                            startDestination = AppDestinations.MAIN.route,
                            modifier = Modifier.weight(9f),
                            contentAlignment = Alignment.Center
                        ) {
                            composable(route = AppDestinations.MAIN.route) {
                                MainScreen(navToGrammar = {navToGrammarActivity(null)}, navToAutomata = {navToAutomataActivity(null)}, navToExamplesScreen = {
                                    navigate(AppDestinations.EXAMPLES.route)}, navToAbout = {navigate(AppDestinations.ABOUT.route)})
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
                            composable(route = AppDestinations.ABOUT.route) {
                                AboutScreen(navBack = {navigate(AppDestinations.MAIN.route)})
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navToGrammarActivity(uri: String?){
        val intent = Intent(this, GrammarActivity::class.java)
        uri?.let { intent.putExtra("example uri", uri) }
        startActivity(intent)
    }

    private fun navToAutomataActivity(uri: String?){
        val intent = Intent(this, AutomataActivity::class.java)
        uri?.apply { intent.putExtra("example uri", this) }
        startActivity(intent)
    }
}