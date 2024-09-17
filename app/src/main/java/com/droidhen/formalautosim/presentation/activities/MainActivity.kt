package com.droidhen.formalautosim.presentation.activities

import android.annotation.SuppressLint
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidhen.formalautosim.core.entities.machines.FiniteMachine
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition
import com.droidhen.formalautosim.presentation.navigation.Destinations
import com.droidhen.formalautosim.presentation.navigation.screens.CommunityScreen
import com.droidhen.formalautosim.presentation.navigation.screens.GraphEditorScreen
import com.droidhen.formalautosim.presentation.navigation.screens.MainScreen
import com.droidhen.formalautosim.presentation.navigation.screens.UserProfileScreen
import com.droidhen.formalautosim.presentation.theme.FormalAutoSimTheme
import com.droidhen.formalautosim.presentation.views.BottomBar
import com.droidhen.formalautosim.utils.extensions.SetDefaultSettings
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var hideStatusBar = mutableIntStateOf(0)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testFiniteStateMachine()


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
                            startDestination = Destinations.MAIN.route,
                            modifier = Modifier.weight(9f)
                        ) {
                            composable(route = Destinations.MAIN.route) {
                                MainScreen()
                            }
                            composable(route = Destinations.COMMUNITY.route) {
                                CommunityScreen()
                            }
                            composable(route = Destinations.USER_PROFILE.route) {
                                UserProfileScreen()
                            }
                            composable(route = Destinations.GRAPH_EDITOR.route) {
                                GraphEditorScreen()
                            }
                        }
                        BottomBar(this@apply)
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideStatusBar.intValue += 1
    }

    private fun testFiniteStateMachine(){
        val firstState =  State(
            finite = true,
            initial = true,
            index = 1,
            name = "a",
            isCurrent = true
        )

        val secondState = State(
            finite = false,
            initial = false,
            index = 2,
            name = "b"
        )

        firstState.position = Offset(100f, 100f)
        secondState.position = Offset(280f, 300f)

        TestMachine.addNewState(
           firstState
        )
        TestMachine.addNewState(
            secondState
        )
        TestMachine.addTransition(Transition(startState  =1, endState = 2))
        TestMachine.addTransition(Transition(startState = 2, endState =  1))
        for(i in 0..30)TestMachine.input.add('a')
    }

    @SuppressLint("ComposableNaming")
    @Composable
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, LocalView.current).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        val TestMachine = FiniteMachine()
    }
}