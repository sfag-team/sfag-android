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
import androidx.compose.material3.Text
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
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.usecase.validation.isDeterministicFinite
import com.sfag.automata.presentation.viewmodel.AutomataViewModel
import com.sfag.automata.presentation.viewmodel.CurrentMachine
import com.sfag.shared.theme.light_blue
import com.sfag.automata.ui.component.common.DefaultDialogWindow
import com.sfag.automata.ui.component.common.DefaultTextField
import com.sfag.automata.ui.component.edit.EditingInput
import com.sfag.automata.ui.component.edit.EditingMachine
import com.sfag.automata.ui.component.edit.EditingMachineBottom
import com.sfag.automata.ui.component.simulation.AnimationOfTransition
import com.sfag.automata.ui.component.simulation.MultipleAnimationsOfTransition
import com.sfag.automata.ui.component.simulation.SimulateMachine
import com.sfag.automata.ui.component.visualization.DerivationTree
import com.sfag.automata.ui.component.visualization.MathFormatView
import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.presentation.model.AutomataScreenStates
import com.sfag.R
import com.sfag.automata.data.FileStorage
import com.sfag.shared.ui.component.DefaultButton
import com.sfag.shared.ui.component.ImmutableTextField

@Composable
fun AutomataScreen(navBack: () -> Unit) {
    val viewModel: AutomataViewModel = hiltViewModel()
    val currentMachine = CurrentMachine.machine
    if (currentMachine == null) {
        navBack()
        return
    }
    val automata by remember {
        mutableStateOf(currentMachine)
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

    // State for DFA/NFA dialog
    var showDeterminismDialog by remember { mutableStateOf(false) }
    var determinismText by remember { mutableStateOf("") }

    BackHandler {
        when (currentScreenState.value) {
            AutomataScreenStates.SIMULATING -> {
                viewModel.saveMachine(automata)
                navBack()
            }

            AutomataScreenStates.SIMULATION_RUN -> {
                // Ignore back during animation
            }

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

                // Top panel with buttons
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val buttonModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()

                    DefaultButton(
                        text = "Back",
                        modifier = buttonModifier
                    ) {
                        viewModel.saveMachine(automata)
                        navBack()
                    }

                    DefaultButton(
                        text = "Export",
                        modifier = buttonModifier
                    ) {
                        exportFileWindow = true
                    }

                    DefaultButton(
                        text = "Share",
                        modifier = buttonModifier
                    ) {
                        FileStorage.shareJffFile(
                            context = context,
                            jffContent = automata.exportToJFF(),
                            filename = automata.name
                        )
                    }

                    DefaultButton(
                        text = "DFA",
                        modifier = buttonModifier
                    ) {
                        determinismText = when (automata.machineType) {
                            MachineType.Finite ->
                                if (automata.isDeterministicFinite())
                                    "The automaton is deterministic (DFA)."
                                else
                                    "The automaton is nondeterministic (NFA)."
                            MachineType.Pushdown ->
                                "This is a pushdown automaton (PDA). DFA/NFA check does not apply."
                            MachineType.Turing ->
                                "This is a Turing machine (TM). DFA/NFA check does not apply."
                        }
                        showDeterminismDialog = true
                    }

                }

                // Canvas / main automaton area
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
                        AutomataScreenStates.SIMULATING -> {
                            key(recompose.intValue) {
                                automata.SimulateMachine(onEditInputClick = { currentScreenState.value = AutomataScreenStates.EDITING_INPUT })
                            }
                        }

                        AutomataScreenStates.SIMULATION_RUN -> {
                            key(animation.intValue) {
                                if (!isLockedAnimation) {
                                    when (val result = automata.calculateNextStep()) {
                                        is SimulationResult.Ended -> {
                                            isLockedAnimation = true
                                            recompose.intValue++
                                            currentScreenState.value = AutomataScreenStates.SIMULATING
                                            val message = when (result.accepted) {
                                                true -> {
                                                    val acceptingStates = automata.states
                                                        .filter { it.isCurrent && it.finite }
                                                        .joinToString(", ") { it.name }
                                                    "Accepted! In accepting state(s): $acceptingStates"
                                                }
                                                false -> {
                                                    val currentStatesStr = automata.states
                                                        .filter { it.isCurrent }
                                                        .joinToString(", ") { it.name }
                                                    if (automata.input.isEmpty()) {
                                                        "Rejected. Stopped in non-accepting state(s): $currentStatesStr"
                                                    } else {
                                                        "Rejected. No valid transitions. Remaining input: ${automata.input}"
                                                    }
                                                }
                                                null -> null
                                            }
                                            message?.let {
                                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        is SimulationResult.Transition -> {
                                            automata.AnimationOfTransition(
                                                start = result.startPosition,
                                                end = result.endPosition,
                                                radius = result.radius,
                                                onAnimationEnd = {
                                                    result.onComplete()
                                                    isLockedAnimation = true
                                                    recompose.intValue++
                                                    currentScreenState.value = AutomataScreenStates.SIMULATING
                                                }
                                            )
                                        }
                                        is SimulationResult.MultipleTransitions -> {
                                            // NFA: animate all transitions in parallel
                                            automata.MultipleAnimationsOfTransition(
                                                transitions = result.transitions,
                                                onAllAnimationsEnd = {
                                                    result.onAllComplete()
                                                    isLockedAnimation = true
                                                    recompose.intValue++
                                                    currentScreenState.value = AutomataScreenStates.SIMULATING
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            key(recompose.intValue) {
                                automata.SimulateMachine(onEditInputClick = { currentScreenState.value = AutomataScreenStates.EDITING_INPUT })
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

                // Bottom navigation row (Editing Machine, Editing Input, TestMachine)
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
                            currentScreenState.value = AutomataScreenStates.EDITING_MACHINE
                        }
                    )
                    Spacer(modifier = Modifier.width(36.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.input_ic),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            currentScreenState.value = AutomataScreenStates.EDITING_INPUT
                        }
                    )
                    Spacer(modifier = Modifier.width(36.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.go_to_next),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            if (currentScreenState.value != AutomataScreenStates.SIMULATING &&
                                currentScreenState.value != AutomataScreenStates.SIMULATION_RUN
                            ) {
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
                        }
                    )
                }

                Spacer(modifier = Modifier.size(24.dp))

                BottomScreenPart(currentScreenState, automata, bottomRecompose = recompose)

                Spacer(modifier = Modifier.size(30.dp))
            }
        }

        // Export dialog
        key(exportFileWindow) {
            if (exportFileWindow) {
                ExportWindow(automata) {
                    exportFileWindow = false
                }
            }
        }

        // DFA/NFA result dialog
        if (showDeterminismDialog) {
            DefaultDialogWindow(
                title = "Automaton Determinism",
                onDismiss = { showDeterminismDialog = false },
                onConfirm = { showDeterminismDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = determinismText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom part - displays derivation tree or editing bottom UI based on state
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
            MathFormatView(automata.getMathFormatData())
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

    DefaultDialogWindow(
        title = "export machine as .jff",
        onDismiss = finished,
        onConfirm = {
            FileStorage.saveJffToDownloads(
                context, jFFMachine, filename
            )
            finished()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.size(40.dp))
            DefaultTextField(
                modifier = Modifier.fillMaxWidth(0.7f),
                hint = "file name",
                value = filename,
                requirementText = "file name: without ' ', '%', '.'",
                onTextChange = {
                    filename = it
                },
                isRequirementsComplete = {
                    filename.let { !(it.contains(' ') || it.contains('%') || it.contains('.')) }
                }
            )
            Spacer(modifier = Modifier.size(40.dp))
            ImmutableTextField(
                text = "File will be saved to downloads",
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}
