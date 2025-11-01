package com.sfag.main.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sfag.automata.theme.Theme
import com.sfag.automata.ui.activity.AutomataActivity
import com.sfag.automata.ui.screen.AboutScreen
import com.sfag.automata.ui.screen.ExamplesScreen
import com.sfag.automata.ui.activity.SetDefaultSettings
import com.sfag.grammar.ui.activity.GrammarActivity
import com.sfag.main.ui.screen.MainScreen
import com.sfag.shared.ui.navigation.Destinations
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Theme {
                SetDefaultSettings()

                rememberNavController().apply {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        NavHost(
                            navController = this@apply,
                            startDestination = Destinations.MAIN.route,
                            modifier = Modifier.weight(9f),
                            contentAlignment = Alignment.Center
                        ) {
                            composable(route = Destinations.MAIN.route) {
                                MainScreen(navToGrammar = {navToGrammarActivity(null)}, navToAutomata = {navToAutomataActivity(null)}, navToExamplesScreen = {
                                    navigate(Destinations.EXAMPLES.route)}, navToAbout = {navigate(Destinations.ABOUT.route)})
                            }
                            composable(route = Destinations.EXAMPLES.route){
                                ExamplesScreen(navBack = {
                                    navigate(Destinations.MAIN.route)
                                },
                                    navToGrammar = { grammarUri ->
                                        navToGrammarActivity(grammarUri)
                                    }, navToAutomata = { automataUri ->
                                        navToAutomataActivity(automataUri)
                                    })
                            }
                            composable(route = Destinations.ABOUT.route) {
                                AboutScreen(navBack = {navigate(Destinations.MAIN.route)})
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
