package com.sfag.home.ui

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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

import com.sfag.automata.ui.AutomataActivity
import com.sfag.grammar.ui.GrammarActivity
import com.sfag.shared.EXTRA_EXAMPLE_NAME
import com.sfag.shared.EXTRA_EXAMPLE_URI
import com.sfag.shared.ui.configureScreenOrientation
import com.sfag.shared.ui.theme.AppTheme

enum class HomeDestinations(val route: String) {
    MAIN("mainScreen"),
    EXAMPLES("examplesScreen"),
    ABOUT("aboutScreen")
}

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
                                composable(route = HomeDestinations.EXAMPLES.route) {
                                    ExamplesScreen(
                                        navBack = {
                                            navigate(HomeDestinations.MAIN.route) {
                                                popUpTo(HomeDestinations.MAIN.route) { inclusive = true }
                                            }
                                        },
                                        navToGrammar = { grammarUri, name ->
                                            navigate(HomeDestinations.MAIN.route) {
                                                popUpTo(HomeDestinations.MAIN.route) { inclusive = true }
                                            }
                                            navToGrammarActivity(grammarUri, name)
                                        },
                                        navToAutomata = { automataUri, name ->
                                            navigate(HomeDestinations.MAIN.route) {
                                                popUpTo(HomeDestinations.MAIN.route) { inclusive = true }
                                            }
                                            navToAutomataActivity(automataUri, name)
                                        }
                                    )
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

    private fun navToGrammarActivity(uri: String?, name: String? = null) {
        val intent = Intent(this, GrammarActivity::class.java)
        uri?.let { intent.putExtra(EXTRA_EXAMPLE_URI, uri) }
        name?.let { intent.putExtra(EXTRA_EXAMPLE_NAME, it) }
        startActivity(intent)
    }

    private fun navToAutomataActivity(uri: String?, name: String? = null) {
        val intent = Intent(this, AutomataActivity::class.java)
        uri?.apply { intent.putExtra(EXTRA_EXAMPLE_URI, this) }
        name?.let { intent.putExtra(EXTRA_EXAMPLE_NAME, it) }
        startActivity(intent)
    }
}
