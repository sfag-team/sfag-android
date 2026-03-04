package com.sfag.automata.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.model.machine.FiniteMachine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.TuringTransition
import com.sfag.automata.ui.viewmodel.AutomataViewModel
import com.sfag.shared.ui.theme.AppTheme
import androidx.activity.viewModels
import com.sfag.automata.ui.screen.AutomataScreen
import com.sfag.automata.data.JffParser
import com.sfag.shared.ui.activity.configureScreenOrientation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AutomataActivity : ComponentActivity() {

    private val viewModel: AutomataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra("example uri")?.let { uri ->
            val content = assets.open(uri).bufferedReader().use { it.readText() }
            val parseResult = JffParser.parseJffWithType(content)
            val machine = when (parseResult.machineType) {
                MachineType.Finite -> FiniteMachine(
                    name = "untitled",
                    states = parseResult.states.toMutableList(),
                    transitions = parseResult.transitions.toMutableList()
                )
                MachineType.Pushdown -> PushDownMachine(
                    name = "untitled",
                    states = parseResult.states.toMutableList(),
                    transitions = parseResult.transitions.filterIsInstance<PushDownTransition>().toMutableList()
                )
                MachineType.Turing -> TuringMachine(
                    name = "untitled",
                    states = parseResult.states.toMutableList(),
                    transitions = parseResult.transitions.filterIsInstance<TuringTransition>().toMutableList()
                )
            }
            viewModel.setCurrentMachine(machine, parseResult.positions)
        } ?: run {
            if (viewModel.currentMachine == null) {
                if (!viewModel.loadCurrentMachine()) {
                    viewModel.setCurrentMachine(FiniteMachine(name = "untitled"))
                }
            }
        }

        configureScreenOrientation()

        setContent {
            AppTheme {
                val machine = viewModel.currentMachine
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    containerColor = colorScheme.surfaceContainerLowest,
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                shape = MaterialTheme.shapes.small,
                                containerColor = colorScheme.inverseSurface,
                                contentColor = colorScheme.inverseOnSurface,
                                modifier = Modifier
                                    .padding(horizontal = 48.dp, vertical = 16.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    key(machine) {
                        AutomataScreen(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            snackbarHostState = snackbarHostState,
                            navBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}
