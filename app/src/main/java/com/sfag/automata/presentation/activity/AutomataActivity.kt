package com.sfag.automata.presentation.activity

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
import com.sfag.automata.domain.model.machine.FiniteMachine
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.TuringTransition
import com.sfag.automata.presentation.viewmodel.CurrentMachine
import com.sfag.shared.presentation.theme.AppTheme
import com.sfag.automata.presentation.navigation.AutomataDestinations
import com.sfag.automata.presentation.screen.AutomataListScreen
import com.sfag.automata.presentation.screen.AutomataScreen
import com.sfag.automata.data.JffParser
import com.sfag.shared.presentation.activity.configureScreenOrientation
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
                val (states, transitions) = JffParser.parseJff(jffXml)
                val typeTag = jffXml.split("<type>")[1].split("</type>")[0].trim().lowercase()
                val machineType = MachineType.fromTag(typeTag)
                exampleMachine = when (machineType) {
                    MachineType.Finite -> FiniteMachine(
                        name = "example finite",
                        states = states.toMutableList(),
                        transitions = transitions.toMutableList()
                    )
                    MachineType.Pushdown -> PushDownMachine(
                        name = "example pushdown",
                        states = states.toMutableList(),
                        transitions = transitions.filterIsInstance<PushDownTransition>().toMutableList()
                    )
                    MachineType.Turing -> TuringMachine(
                        name = "example turing",
                        states = states.toMutableList(),
                        transitions = transitions.filterIsInstance<TuringTransition>().toMutableList()
                    )
                }
            }
        }

        configureScreenOrientation()

        setContent {

            AppTheme {
                rememberNavController().apply {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = this@apply,
                                startDestination = AutomataDestinations.AUTOMATA_LIST.route,
                                modifier = Modifier.weight(9f),
                                contentAlignment = Alignment.Center
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
}
