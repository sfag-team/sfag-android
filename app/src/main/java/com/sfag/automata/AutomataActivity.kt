package com.sfag.automata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sfag.automata.data.Jff
import com.sfag.automata.data.toMachine
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.ui.AutomataScreen
import com.sfag.automata.ui.AutomataViewModel
import com.sfag.main.config.EXTRA_EXAMPLE_NAME
import com.sfag.main.config.EXTRA_EXAMPLE_URI
import com.sfag.main.config.EXTRA_NEW_MACHINE_NAME
import com.sfag.main.config.EXTRA_NEW_MACHINE_TYPE
import com.sfag.main.configureScreenOrientation
import com.sfag.main.ui.component.DefaultSnackbarHost
import com.sfag.main.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AutomataActivity : ComponentActivity() {
    private val viewModel: AutomataViewModel by viewModels()

    override fun onStop() {
        super.onStop()
        viewModel.currentMachine?.let { viewModel.autoSave(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // Fresh launch — load from intent or storage
            intent.getStringExtra(EXTRA_EXAMPLE_URI)?.let { uri ->
                val jff = assets.open(uri).use { Jff.parse(it) }
                val exampleName = intent.getStringExtra(EXTRA_EXAMPLE_NAME) ?: "untitled"
                viewModel.setCurrentMachine(
                    jff.toMachine(exampleName),
                    jff.positions,
                )
            }
                ?: intent.getStringExtra(EXTRA_NEW_MACHINE_TYPE)?.let { machineType ->
                    val machineName = intent.getStringExtra(EXTRA_NEW_MACHINE_NAME) ?: "untitled"
                    val newMachine =
                        if (machineType == "pushdown") {
                            PushdownMachine(name = machineName)
                        } else {
                            FiniteMachine(name = machineName)
                        }
                    viewModel.setCurrentMachine(newMachine)
                }
                ?: run {
                    if (viewModel.currentMachine == null) {
                        viewModel.loadMachine()
                    }
                }
        } else if (viewModel.currentMachine == null) {
            // Process death recreation — ViewModel lost, reload from storage
            viewModel.loadMachine()
        }

        configureScreenOrientation()

        setContent {
            AppTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    containerColor = colorScheme.surfaceContainerLowest,
                    snackbarHost = { DefaultSnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    AutomataScreen(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        snackbarHostState = snackbarHostState,
                        navBack = { finish() },
                    )
                }
            }
        }
    }
}
