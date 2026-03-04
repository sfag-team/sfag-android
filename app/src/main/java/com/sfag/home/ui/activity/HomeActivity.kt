package com.sfag.home.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sfag.shared.ui.theme.AppTheme
import com.sfag.automata.ui.activity.AutomataActivity
import com.sfag.grammar.ui.activity.GrammarActivity
import com.sfag.home.ui.screen.AboutScreen
import com.sfag.home.ui.screen.ExamplesScreen
import com.sfag.home.ui.screen.HomeScreen
import com.sfag.home.ui.navigation.HomeDestinations
import com.sfag.shared.ui.activity.configureScreenOrientation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureScreenOrientation()

        setContent {
            AppTheme {

                rememberNavController().apply {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = this@apply,
                                startDestination = HomeDestinations.MAIN.route,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                composable(route = HomeDestinations.MAIN.route) {
                                    HomeScreen(navToGrammar = {navToGrammarActivity(null)}, navToAutomata = {navToAutomataActivity(null)}, navToExamplesScreen = {
                                        navigate(HomeDestinations.EXAMPLES.route)}, navToAbout = {navigate(HomeDestinations.ABOUT.route)})
                                }
                                composable(route = HomeDestinations.EXAMPLES.route){
                                    ExamplesScreen(navBack = {
                                        navigate(HomeDestinations.MAIN.route)
                                    },
                                        navToGrammar = { grammarUri ->
                                            navToGrammarActivity(grammarUri)
                                        }, navToAutomata = { automataUri ->
                                            navToAutomataActivity(automataUri)
                                        })
                                }
                                composable(route = HomeDestinations.ABOUT.route) {
                                    AboutScreen(navBack = {navigate(HomeDestinations.MAIN.route)})
                                }
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
