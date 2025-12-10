package com.sfag.automata.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sfag.automata.domain.model.machine.FiniteMachine
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.presentation.viewmodel.CurrentMachine
import com.sfag.shared.theme.AppTheme
import com.sfag.automata.navigation.AutomataDestinations
import com.sfag.automata.ui.screen.AutomataListScreen
import com.sfag.automata.ui.screen.AutomataScreen
import com.sfag.automata.data.FileStorage
import com.sfag.shared.ui.activity.SetDefaultSettings
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AutomataActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var exampleMachine: Machine? = null

        intent.getStringExtra("example uri")?.let { it ->
            CurrentMachine.machine = null
            val inputStream = assets.open(it)
            val content = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            content.let { jffXml ->
                val (states, transitions) = FileStorage.parseJff(jffXml)
                val machineType: MachineType =
                    if (jffXml.split("<type>")[1].split("</type>")[0] == MachineType.Finite.tag
                    ) MachineType.Finite else MachineType.Pushdown
                val machine = if (machineType == MachineType.Finite) FiniteMachine(
                    name = "example Finite",
                    states = states.toMutableList(),
                    transitions = transitions.toMutableList()
                ) else PushDownMachine(
                    name = "example PDA",
                    states = states.toMutableList(),
                    transitions = transitions.filterIsInstance<PushDownTransition>().toMutableList()
                )
                exampleMachine = machine
            }
        }

        setContent {

            AppTheme {
                SetDefaultSettings()
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
                        }
                    }
                }
            }
        }
    }
}
