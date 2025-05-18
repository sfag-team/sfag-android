package com.droidhen.formalautosim.presentation.activities

import android.annotation.SuppressLint
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
import com.droidhen.formalautosim.core.entities.machines.FiniteMachine
import com.droidhen.formalautosim.core.entities.machines.Machine
import com.droidhen.formalautosim.core.entities.machines.MachineType
import com.droidhen.formalautosim.core.entities.machines.PushDownMachine
import com.droidhen.formalautosim.core.entities.machines.PushDownTransition
import com.droidhen.formalautosim.core.viewModel.CurrentMachine
import com.droidhen.formalautosim.data.local.ExternalStorageController
import com.droidhen.formalautosim.presentation.navigation.AutomataDestinations
import com.droidhen.formalautosim.presentation.navigation.screens.AutomataScreen
import com.droidhen.formalautosim.presentation.navigation.screens.CommunityScreen
import com.droidhen.formalautosim.presentation.navigation.screens.UserProfileScreen
import com.droidhen.formalautosim.presentation.theme.FormalAutoSimTheme
import com.droidhen.formalautosim.presentation.views.BottomBar
import com.droidhen.formalautosim.utils.extensions.SetDefaultSettings
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AutomataActivity : ComponentActivity() {
    private var hideStatusBar = mutableIntStateOf(0)

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var exampleMachine: Machine? = null

        intent.getStringExtra("example uri")?.let {
            CurrentMachine.machine = null
            val inputStream = assets.open(it)
            val content = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            content.let { jffXml ->
                val (states, transitions) = ExternalStorageController().parseJff(jffXml)
                val machineType: MachineType =
                    if (jffXml.split("<type>").get(1).split("</type>").get(0)
                            .equals(MachineType.Finite.tag)
                    ) MachineType.Finite else MachineType.Pushdown
                val machine = if (machineType == MachineType.Finite) FiniteMachine(
                    name = "example Finite",
                    states = states.toMutableList(),
                    transitions = transitions.toMutableList()
                ) else PushDownMachine(
                    name = "example PDA",
                    states = states.toMutableList(),
                    transitions = transitions.toMutableList() as MutableList<PushDownTransition>
                )
                exampleMachine = machine
            }
        }

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
                            startDestination = AutomataDestinations.AUTOMATA_LIST.route,
                            modifier = Modifier.weight(9f)
                        ) {
                            composable(route = AutomataDestinations.AUTOMATA.route) {
                                AutomataScreen {
                                    navigate(AutomataDestinations.AUTOMATA_LIST.route)
                                }
                            }
                            composable(route = AutomataDestinations.AUTOMATA_LIST.route) {
                                AutomataListScreen(exampleMachine, navBack = {
                                    finish()
                                }) {
                                    navigate(AutomataDestinations.AUTOMATA.route)
                                }
                            }
                            composable(route = AutomataDestinations.COMMUNITY.route) {
                                CommunityScreen()
                            }
                            composable(route = AutomataDestinations.USER_PROFILE.route) {
                                UserProfileScreen()
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

    @SuppressLint("ComposableNaming")
    @Composable
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, LocalView.current).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

}