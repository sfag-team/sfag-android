package com.sfag.automata.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

import com.sfag.automata.data.JffParser
import com.sfag.automata.data.toMachine
import com.sfag.shared.EXTRA_EXAMPLE_NAME
import com.sfag.shared.EXTRA_EXAMPLE_URI
import com.sfag.shared.ui.component.DefaultSnackbarHost
import com.sfag.shared.ui.configureScreenOrientation
import com.sfag.shared.ui.theme.AppTheme

@AndroidEntryPoint
class AutomataActivity : ComponentActivity() {

    private val viewModel: AutomataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(EXTRA_EXAMPLE_URI)?.let { uri ->
            val content = assets.open(uri).bufferedReader().use { it.readText() }
            val parseResult = JffParser.parseJffWithType(content)
            val exampleName = intent.getStringExtra(EXTRA_EXAMPLE_NAME) ?: "untitled"
            val machine = parseResult.toMachine(exampleName)
            viewModel.setCurrentMachine(machine, parseResult.positions)
        } ?: run {
            if (viewModel.currentMachine == null) {
                viewModel.loadCurrentMachine()
            }
        }

        configureScreenOrientation()

        setContent {
            AppTheme {
                val machine = viewModel.currentMachine
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    containerColor = colorScheme.surfaceContainerLowest,
                    snackbarHost = { DefaultSnackbarHost(snackbarHostState) }
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
