package com.sfag.automata.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.automata.core.machine.Machine
import com.sfag.automata.core.machine.MachineType
import com.sfag.automata.core.viewmodel.AutomataViewModel
import com.sfag.automata.core.viewmodel.CurrentMachine
import com.sfag.automata.theme.light_blue
import com.sfag.automata.ui.common.DefaultDialogWindow
import com.sfag.automata.ui.common.DefaultTextField
import com.sfag.automata.ui.common.ImmutableTextField
import com.sfag.automata.ui.edit.EditingInput
import com.sfag.automata.ui.edit.EditingMachine
import com.sfag.automata.ui.edit.EditingMachineBottom
import com.sfag.automata.ui.simulation.SimulateMachine
import com.sfag.automata.ui.visualization.DerivationTree
import com.sfag.automata.util.enum.AutomataScreenStates
import com.sfag.R
import com.sfag.shared.data.local.FileStorage
import com.sfag.shared.ui.common.DefaultButton


@Composable
fun AutomataScreen(navBack: () -> Unit) {
    val viewModel: AutomataViewModel = hiltViewModel()
    val automata by remember {
        mutableStateOf(CurrentMachine.machine!!)
    }

    val recompose = remember {
        mutableIntStateOf(0)
    }
    val animation = remember {
        mutableIntStateOf(0)
    }
    val currentScreenState = remember {
        mutableStateOf(AutomataScreenStates.SIMULATING)
    }
    var isLockedAnimation = true

    var exportFileWindow by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current

    BackHandler {
        when (currentScreenState.value) {
            AutomataScreenStates.SIMULATING -> {
                viewModel.saveMachine(automata)
                navBack()
            }

            AutomataScreenStates.SIMULATION_RUN -> {}

            AutomataScreenStates.EDITING_INPUT -> {
                viewModel.saveMachine(automata)
                currentScreenState.value = AutomataScreenStates.SIMULATING
            }

            AutomataScreenStates.EDITING_MACHINE -> {
                viewModel.saveMachine(automata)
                currentScreenState.value = AutomataScreenStates.SIMULATING
            }

        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(30.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DefaultButton(text = "Back") {
                        navBack()
                    }
                    DefaultButton(text = "Export") {
                        exportFileWindow = true
                    }
                    DefaultButton(text = "Share") {
                        FileStorage.shareJffFile(
                            context = context,
                            jffContent = automata.exportToJFF(),
                            filename = automata.name
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (automata.machineType == MachineType.Finite) 500.dp else 600.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large
                        )
                        .clip(MaterialTheme.shapes.large)
                ) {
                    when (currentScreenState.value) {
                        AutomataScreenStates.SIMULATING -> {
                            key(recompose.intValue) {
                                automata.SimulateMachine()
                            }
                        }

                        AutomataScreenStates.SIMULATION_RUN -> {
                            key(animation.intValue) {
                                if (isLockedAnimation.not()) {
                                    automata.calculateTransition { state ->
                                        isLockedAnimation = true
                                        recompose.intValue++
                                        currentScreenState.value = AutomataScreenStates.SIMULATING
                                        if (state != null && state) {
                                                Toast.makeText(
                                                    context,
                                                    "Your machine finished successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else if(state!=null) {
                                                Toast.makeText(
                                                    context,
                                                    "Your machine failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                        }
                                    }
                                }
                            }
                            key(recompose.intValue) {
                                automata.SimulateMachine()
                            }
                        }

                        AutomataScreenStates.EDITING_INPUT -> {
                            automata.EditingInput {
                                currentScreenState.value = AutomataScreenStates.SIMULATING
                            }
                        }

                        AutomataScreenStates.EDITING_MACHINE -> {
                            automata.EditingMachine {
                                recompose.intValue++
                            }
                        }
                    }

                }
                Spacer(modifier = Modifier.size(18.dp))

                /**
                 * Bottom navigation row (Editing Machine, Editing Input, TestMachine)
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .clip(MaterialTheme.shapes.large)
                        .border(
                            2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large
                        )
                        .background(light_blue),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(painter = painterResource(id = R.drawable.editing_machine),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            currentScreenState.value = AutomataScreenStates.EDITING_MACHINE
                        })
                    Spacer(modifier = Modifier.width(36.dp))
                    Icon(painter = painterResource(id = R.drawable.input_ic),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            currentScreenState.value = AutomataScreenStates.EDITING_INPUT
                        })
                    Spacer(modifier = Modifier.width(36.dp))
                    Icon(painter = painterResource(id = R.drawable.go_to_next),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            if (currentScreenState.value != AutomataScreenStates.SIMULATING && currentScreenState.value != AutomataScreenStates.SIMULATION_RUN) {
                                currentScreenState.value = AutomataScreenStates.SIMULATING
                            } else if (currentScreenState.value == AutomataScreenStates.SIMULATING) {
                                currentScreenState.value = AutomataScreenStates.SIMULATION_RUN
                                if (isLockedAnimation) {
                                    isLockedAnimation = false
                                    animation.intValue++
                                }
                            } else {
                                if (isLockedAnimation) {
                                    isLockedAnimation = false
                                    animation.intValue++
                                }
                            }
                        })
                }

                Spacer(modifier = Modifier.size(24.dp))

                BottomScreenPart(currentScreenState, automata, bottomRecompose = recompose)

                Spacer(modifier = Modifier.size(30.dp))
            }
        }

        key(exportFileWindow) {
            if (exportFileWindow) {
                ExportWindow(automata) {
                    exportFileWindow = false
                }
            }
        }
    }
}

/**
 *
 * Shows additional info for related screen state  (ex.: for simulating it shows derivation tree and shows interface for multipling testing)
 */
@Composable
private fun BottomScreenPart(
    currentScreenState: MutableState<AutomataScreenStates>,
    automata: Machine,
    bottomRecompose: MutableIntState
) {
    when (currentScreenState.value) {
        AutomataScreenStates.SIMULATING -> {
            automata.DerivationTree()
            Spacer(modifier = Modifier.size(32.dp))
            automata.MathFormat()
        }

        AutomataScreenStates.EDITING_MACHINE -> {
            automata.EditingMachineBottom(bottomRecompose)
        }

        else -> {}
    }
}

@Composable
private fun ExportWindow(machine: Machine, finished: () -> Unit = {}) {
    var filename by remember {
        mutableStateOf("AS_${machine.name}_version_${machine.version}")
    }
    val jFFMachine = machine.exportToJFF()

    val context = LocalContext.current

    DefaultDialogWindow(title = "export machine as .jff", onDismiss = finished, onConfirm = {
        FileStorage.saveJffToDownloads(
            context, jFFMachine, filename
        )
        finished()
    }) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.size(40.dp))
            DefaultTextField(modifier = Modifier.fillMaxWidth(0.7f),
                hint = "file name",
                value = filename,
                requirementText = "file name: without ' ', '%', '.'",
                onTextChange = {
                    filename = it
                },
                isRequirementsComplete = {
                    filename.let { !(it.contains(' ') || it.contains('%') || it.contains('.')) }
                })
            Spacer(modifier = Modifier.size(40.dp))
            ImmutableTextField(
                text = "File will be saved to downloads", modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}
