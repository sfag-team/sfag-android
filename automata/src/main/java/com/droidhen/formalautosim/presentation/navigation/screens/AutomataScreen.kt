package com.droidhen.formalautosim.presentation.navigation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidhen.automata.R
import com.droidhen.formalautosim.core.entities.machines.Machine
import com.droidhen.formalautosim.core.entities.machines.MachineType
import com.droidhen.formalautosim.core.viewModel.AutomataViewModel
import com.droidhen.formalautosim.core.viewModel.CurrentMachine
import com.droidhen.formalautosim.data.local.ExternalStorageController
import com.droidhen.formalautosim.presentation.activities.AutomataActivity
import com.droidhen.formalautosim.presentation.theme.light_blue
import com.droidhen.formalautosim.utils.enums.MainScreenStates
import views.DefaultFASDialogWindow
import views.FASButton
import views.FASDefaultTextField
import views.FASImmutableTextField

@Composable
fun AutomataScreen(navBack: ()->Unit) {
    val viewModel:AutomataViewModel = hiltViewModel()
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
        mutableStateOf(MainScreenStates.SIMULATING)
    }
    var isLockedAnimation = true

    var exportFileWindow by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current

    BackHandler {
        when (currentScreenState.value) {
            MainScreenStates.SIMULATING -> {
                viewModel.saveMachine(automata)
                navBack()
            }

            MainScreenStates.SIMULATION_RUN -> {}

            MainScreenStates.EDITING_INPUT -> {
                currentScreenState.value = MainScreenStates.SIMULATING
            }

            MainScreenStates.EDITING_MACHINE -> {
                currentScreenState.value = MainScreenStates.SIMULATING
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
                    FASButton(text = "Export") {
                        exportFileWindow = true
                    }
                    FASButton(text = "Share") {
                        ExternalStorageController.shareJffFile(context = context, jffContent = automata.exportToJFF(), fileName = automata.name)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (automata.machineType == MachineType.Finite) 500.dp else 600.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.shapes.large
                        )
                        .clip(MaterialTheme.shapes.large)
                ) {
                    when (currentScreenState.value) {
                        MainScreenStates.SIMULATING -> {
                            key(recompose.intValue) {
                                automata.SimulateMachine()
                            }
                        }

                        MainScreenStates.SIMULATION_RUN -> {
                            key(animation.intValue) {
                                if (isLockedAnimation.not()) {
                                    automata.calculateTransition {
                                        isLockedAnimation = true
                                        recompose.intValue++
                                        currentScreenState.value = MainScreenStates.SIMULATING
                                    }
                                }
                            }
                            key(recompose.intValue) {
                                automata.SimulateMachine()
                            }
                        }

                        MainScreenStates.EDITING_INPUT -> {
                            automata.EditingInput {
                                currentScreenState.value = MainScreenStates.SIMULATING
                            }
                        }

                        MainScreenStates.EDITING_MACHINE -> {
                            automata.EditingMachine{
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
                            2.dp,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.shapes.large
                        )
                        .background(light_blue),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.editing_machine),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            currentScreenState.value = MainScreenStates.EDITING_MACHINE
                        })
                    Spacer(modifier = Modifier.width(36.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.input_ic),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            currentScreenState.value = MainScreenStates.EDITING_INPUT
                        })
                    Spacer(modifier = Modifier.width(36.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.go_to_next),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            if (currentScreenState.value != MainScreenStates.SIMULATING && currentScreenState.value != MainScreenStates.SIMULATION_RUN) {
                                currentScreenState.value = MainScreenStates.SIMULATING
                            } else if (currentScreenState.value == MainScreenStates.SIMULATING) {
                                currentScreenState.value = MainScreenStates.SIMULATION_RUN
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
                ExportWindow(automata){
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
private fun BottomScreenPart(currentScreenState: MutableState<MainScreenStates>, automata: Machine, bottomRecompose: MutableIntState) {
    when (currentScreenState.value) {
        MainScreenStates.SIMULATING -> {
            automata.DerivationTree()
            Spacer(modifier = Modifier.size(32.dp))
            automata.MathFormat()
        }
        MainScreenStates.EDITING_MACHINE -> {
            automata.EditingMachineBottom(bottomRecompose)
        }

        else -> {}
    }
}

@Composable
private fun ExportWindow(machine: Machine, finished: () -> Unit = {}) {
    var fileName by remember {
        mutableStateOf("AS_${machine.name}_version_${machine.version}")
    }
    val jFFMachine = machine.exportToJFF()

    val context = LocalContext.current

    DefaultFASDialogWindow(
        title = "export machine as .jff",
        onDismiss = finished,
        onConfirm = {
            ExternalStorageController.saveJffToDownloads(
                context,
                jFFMachine,
                fileName
            )
            finished()
        }) {
        Column(modifier = Modifier
            .fillMaxHeight(0.6f)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
            Spacer(modifier = Modifier.size(40.dp))
            FASDefaultTextField(
                modifier = Modifier.fillMaxWidth(0.7f),
                hint = "file name",
                value = fileName,
                requirementText = "file name: without ' ', '%', '.'",
                onTextChange = {
                    fileName = it
                }, isRequirementsComplete = {
                    fileName.let { !(it.contains(' ') || it.contains('%') || it.contains('.')) }
                })
            Spacer(modifier = Modifier.size(40.dp))
            FASImmutableTextField(text = "File will be saved to downloads", modifier = Modifier.fillMaxWidth(0.85f))
        }
    }
}