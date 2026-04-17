package com.sfag.automata.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.R
import com.sfag.automata.data.Jff
import com.sfag.automata.data.exportToJff
import com.sfag.automata.data.toMachine
import com.sfag.automata.domain.common.getFormalDefinition
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.MachineType
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.snapshotActiveNodes
import com.sfag.automata.domain.tree.markSimulationEnd
import com.sfag.automata.ui.common.FormalDefinitionView
import com.sfag.automata.ui.edit.StateList
import com.sfag.automata.ui.edit.TransitionList
import com.sfag.automata.ui.input.InputEditor
import com.sfag.automata.ui.machine.DialogRequest
import com.sfag.automata.ui.machine.MachineEditor
import com.sfag.automata.ui.machine.TransitionAnimation
import com.sfag.automata.ui.machine.computeTransitionPaths
import com.sfag.automata.ui.tree.TreeView
import com.sfag.main.config.JFF_OPEN_MIME_TYPES
import com.sfag.main.config.JFF_SAVE_MIME_TYPE
import com.sfag.main.data.JffUtils
import com.sfag.main.ui.component.CancelButton
import com.sfag.main.ui.component.ConfirmButton
import com.sfag.main.ui.component.CreateButton
import com.sfag.main.ui.component.DefaultButton
import com.sfag.main.ui.component.DefaultDialog
import com.sfag.main.ui.component.DefaultIconButton
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.ItemSpecificationIcon

private enum class Mode {
    SIMULATOR,
    SIMULATION_STEP,
    INPUT_EDITOR,
    MACHINE_EDITOR,
}

private sealed interface ActiveDialog {
    data object NewMachine : ActiveDialog
}

@Composable
fun AutomataScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    navBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: AutomataViewModel = hiltViewModel()

    val importErrorMsg = stringResource(R.string.file_import_error)
    val noInitialStateMsg = stringResource(R.string.no_initial_state)
    val acceptedMsg = stringResource(R.string.accepted_in_states)
    val rejectedMsg = stringResource(R.string.rejected_in_states)

    val machine = viewModel.currentMachine
    if (machine == null) {
        LaunchedEffect(Unit) { navBack() }
        return
    }

    key(machine) {
        val recomposeKey = remember { mutableIntStateOf(0) }
        val currentMode = remember { mutableStateOf(Mode.SIMULATOR) }
        var showUnsavedDialog by remember { mutableStateOf(viewModel.pendingExampleUri != null) }
        var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
        var simulationOutcome by remember { mutableStateOf<SimulationOutcome?>(null) }
        val animationOverlay = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        val dialogRequest = remember { mutableStateOf<DialogRequest?>(null) }
        var snackbarMsg by remember { mutableStateOf<String?>(null) }
        var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }
        val exportErrorMsg = stringResource(R.string.file_export_error)

        val exportLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(JFF_SAVE_MIME_TYPE)
            ) { uri ->
                try {
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { stream ->
                            stream.write(
                                machine
                                    .exportToJff(viewModel.getPositions())
                                    .toByteArray(Charsets.UTF_8)
                            )
                        }
                        machine.name = JffUtils.getJffStem(context, it)
                        recomposeKey.intValue++
                        viewModel.markSaved()
                    }
                } catch (e: Exception) {
                    Log.e("AutomataScreen", "Failed to export file", e)
                    snackbarMsg = exportErrorMsg
                }
            }

        val importLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                uri ->
                uri?.let {
                    try {
                        val machineName = JffUtils.getJffStem(context, it)
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            val jff = Jff.parse(stream)
                            viewModel.setCurrentMachine(jff.toMachine(machineName), jff.positions)
                        }
                    } catch (e: Exception) {
                        Log.e("AutomataScreen", "Failed to import file", e)
                        snackbarMsg = importErrorMsg
                    }
                }
            }

        BackHandler {
            when (currentMode.value) {
                Mode.SIMULATOR -> {
                    if (viewModel.hasUnsavedChanges) {
                        pendingAction = { navBack() }
                        showUnsavedDialog = true
                    } else {
                        viewModel.autoSave(machine)
                        navBack()
                    }
                }

                Mode.SIMULATION_STEP -> {
                    animationOverlay.value = null
                    viewModel.autoSave(machine)
                    navBack()
                }

                Mode.INPUT_EDITOR -> {
                    machine.resetToInitialState()
                    viewModel.autoSave(machine)
                    currentMode.value = Mode.SIMULATOR
                }

                Mode.MACHINE_EDITOR -> {
                    machine.resetToInitialState()
                    viewModel.autoSave(machine)
                    currentMode.value = Mode.SIMULATOR
                    recomposeKey.intValue++
                }
            }
        }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                snackbarMsg = null
            }
        }

        val canvasScrollBlocker = remember {
            object : NestedScrollConnection {
                // Consume vertical scroll only; horizontal passes to tape-bar LazyRows
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                    Offset(0f, available.y)
            }
        }

        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
            if (currentMode.value != Mode.INPUT_EDITOR) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                DefaultButton(
                                    onClick = {
                                        JffUtils.shareFile(
                                            context = context,
                                            jffContent =
                                                machine.exportToJff(viewModel.getPositions()),
                                            name = machine.name,
                                        )
                                    },
                                    text = stringResource(R.string.share_file),
                                    modifier = Modifier.weight(1f),
                                )

                                DefaultButton(
                                    onClick = {
                                        exportLauncher.launch(
                                            JffUtils.addJffExtension(machine.name)
                                        )
                                    },
                                    text = stringResource(R.string.save_file),
                                    modifier = Modifier.weight(1f),
                                )

                                DefaultButton(
                                    onClick = {
                                        if (viewModel.hasUnsavedChanges) {
                                            pendingAction = {
                                                importLauncher.launch(JFF_OPEN_MIME_TYPES)
                                            }
                                            showUnsavedDialog = true
                                        } else {
                                            importLauncher.launch(JFF_OPEN_MIME_TYPES)
                                        }
                                    },
                                    text = stringResource(R.string.import_button),
                                    modifier = Modifier.weight(1f),
                                )

                                DefaultButton(
                                    onClick = {
                                        if (viewModel.hasUnsavedChanges) {
                                            pendingAction = {
                                                activeDialog = ActiveDialog.NewMachine
                                            }
                                            showUnsavedDialog = true
                                        } else {
                                            activeDialog = ActiveDialog.NewMachine
                                        }
                                    },
                                    text = stringResource(R.string.create_new),
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .nestedScroll(canvasScrollBlocker)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                machine.MachineEditor(
                                    isEditing = currentMode.value == Mode.MACHINE_EDITOR,
                                    recomposeKey = recomposeKey.intValue,
                                    animationOverlay = animationOverlay.value,
                                    dialogRequest = dialogRequest,
                                    simulationOutcome = simulationOutcome,
                                    onEdit = {
                                        if (currentMode.value != Mode.SIMULATION_STEP) {
                                            simulationOutcome = null
                                            currentMode.value = Mode.INPUT_EDITOR
                                        }
                                    },
                                    onRecompose = { recomposeKey.intValue++ },
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                DefaultIconButton(
                                    onClick = {
                                        if (currentMode.value == Mode.SIMULATION_STEP) {
                                            return@DefaultIconButton
                                        }
                                        if (currentMode.value == Mode.MACHINE_EDITOR) {
                                            machine.resetToInitialState()
                                            currentMode.value = Mode.SIMULATOR
                                            recomposeKey.intValue++
                                        } else {
                                            currentMode.value = Mode.MACHINE_EDITOR
                                        }
                                    },
                                    icon = R.drawable.edit,
                                    modifier = Modifier.weight(1f),
                                    isActive = currentMode.value == Mode.MACHINE_EDITOR,
                                )
                                DefaultIconButton(
                                    onClick = {
                                        if (currentMode.value == Mode.MACHINE_EDITOR) {
                                            viewModel.machineAutoCenter = true
                                            recomposeKey.intValue++
                                        } else {
                                            simulationOutcome = null
                                            animationOverlay.value = null
                                            viewModel.clearInspection()
                                            machine.resetToInitialState()
                                            currentMode.value = Mode.SIMULATOR
                                            recomposeKey.intValue++
                                        }
                                    },
                                    icon =
                                        if (currentMode.value == Mode.MACHINE_EDITOR) {
                                            R.drawable.center_focus
                                        } else {
                                            R.drawable.replay
                                        },
                                    modifier = Modifier.weight(1f),
                                )
                                DefaultIconButton(
                                    onClick = {
                                        if (currentMode.value == Mode.SIMULATION_STEP) {
                                            return@DefaultIconButton
                                        }
                                        if (machine.states.none { it.initial }) {
                                            snackbarMsg = noInitialStateMsg
                                            return@DefaultIconButton
                                        }
                                        if (currentMode.value != Mode.SIMULATOR) {
                                            currentMode.value = Mode.SIMULATOR
                                        }
                                        when (val simulation = viewModel.advanceSimulation()) {
                                            is Simulation.Ended -> {
                                                machine.tree.markSimulationEnd(
                                                    simulation.isNodeAccepting
                                                )
                                                viewModel.clearInspection()
                                                simulationOutcome = simulation.outcome
                                                recomposeKey.intValue++
                                                snackbarMsg =
                                                    when (simulation.outcome) {
                                                        SimulationOutcome.ACCEPTED -> {
                                                            val stateNames =
                                                                machine.states
                                                                    .filter {
                                                                        it.isCurrent && it.final
                                                                    }
                                                                    .joinToString(", ") { it.name }
                                                            acceptedMsg.format(stateNames)
                                                        }

                                                        SimulationOutcome.REJECTED -> {
                                                            val stateNames =
                                                                machine.states
                                                                    .filter { it.isCurrent }
                                                                    .joinToString(", ") { it.name }
                                                            rejectedMsg.format(stateNames)
                                                        }

                                                        else -> null
                                                    }
                                            }

                                            is Simulation.Step -> {
                                                currentMode.value = Mode.SIMULATION_STEP
                                                recomposeKey.intValue++
                                                val capturedPositions =
                                                    viewModel.statePositions.toMap()
                                                animationOverlay.value = {
                                                    val animDensity = LocalDensity.current
                                                    val transitionPaths =
                                                        machine.computeTransitionPaths(
                                                            capturedPositions,
                                                            animDensity.density,
                                                        )
                                                    TransitionAnimation(
                                                        transitionRefs = simulation.transitionRefs,
                                                        transitionPaths = transitionPaths,
                                                        offsetXCanvas = viewModel.offsetXCanvas,
                                                        offsetYCanvas = viewModel.offsetYCanvas,
                                                        onAnimationsEnd = {
                                                            machine.tree.expandActive(
                                                                simulation.treeBranches
                                                            )
                                                            simulation.onAllComplete()
                                                            machine.tree.attachSnapshots(
                                                                machine.snapshotActiveNodes()
                                                            )
                                                            viewModel.clearInspection()
                                                            animationOverlay.value = null
                                                            currentMode.value = Mode.SIMULATOR
                                                            recomposeKey.intValue++
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    icon = R.drawable.skip_next,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            BottomScreenPart(
                                currentMode,
                                machine,
                                viewModel,
                                recomposeKey = recomposeKey,
                                dialogRequest = dialogRequest,
                            )
                        }
                    }
                }
            }

            // Full-screen input editor overlay - slides up over all other controls
            AnimatedVisibility(
                visible = currentMode.value == Mode.INPUT_EDITOR,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                machine.InputEditor(
                    onConfirm = {
                        viewModel.autoSave(machine)
                        currentMode.value = Mode.SIMULATOR
                    },
                    onDismiss = {
                        viewModel.autoSave(machine)
                        currentMode.value = Mode.SIMULATOR
                    },
                )
            }

            when (activeDialog) {
                is ActiveDialog.NewMachine ->
                    NewMachineWindow { newMachine ->
                        activeDialog = null
                        newMachine?.let { viewModel.setCurrentMachine(it) }
                    }

                null -> {}
            }
            if (showUnsavedDialog) {
                val proceed = {
                    showUnsavedDialog = false
                    val exampleUri = viewModel.pendingExampleUri
                    if (exampleUri != null) {
                        val exampleName = viewModel.pendingExampleName ?: ""
                        viewModel.pendingExampleName = null
                        viewModel.pendingExampleUri = null
                        try {
                            val jff = context.assets.open(exampleUri).use { Jff.parse(it) }
                            viewModel.setCurrentMachine(jff.toMachine(exampleName), jff.positions)
                        } catch (e: Exception) {
                            Log.e("AutomataScreen", "Failed to load example: $exampleUri", e)
                        }
                    } else {
                        pendingAction?.invoke()
                    }
                    pendingAction = null
                }
                DefaultDialog(
                    onDismissRequest = {
                        showUnsavedDialog = false
                        pendingAction = null
                    },
                    title = stringResource(R.string.unsaved_changes),
                    buttons = {
                        CancelButton(
                            onClick = { proceed() },
                            label = stringResource(R.string.discard_button),
                        )
                        ConfirmButton(
                            onClick = {
                                viewModel.autoSave(machine)
                                proceed()
                            }
                        )
                    },
                ) {
                    Text(text = stringResource(R.string.unsaved_changes_message))
                }
            }
        }
    }
}

/** Bottom part - displays derivation tree or editing bottom UI based on mode */
@Composable
private fun BottomScreenPart(
    currentMode: MutableState<Mode>,
    machine: Machine,
    viewModel: AutomataViewModel,
    recomposeKey: MutableIntState,
    dialogRequest: MutableState<DialogRequest?>,
) {
    val isEditingMachine = currentMode.value == Mode.MACHINE_EDITOR
    Crossfade(targetState = isEditingMachine, label = "bottom-panel") { isEditing ->
        if (isEditing) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                machine.StateList(
                    recomposeKey = recomposeKey,
                    onClickState = { state ->
                        dialogRequest.value = DialogRequest.ForState(Offset.Zero, state)
                    },
                    onRemoveState = { state ->
                        viewModel.statePositions.remove(state.index)
                        machine.removeState(state)
                        viewModel.markDirty()
                        recomposeKey.intValue++
                    },
                )
                machine.TransitionList(
                    recomposeKey = recomposeKey,
                    onClickTransition = { transition ->
                        val fromState = machine.getStateByIndex(transition.fromState)
                        val toState = machine.getStateByIndex(transition.toState)
                        dialogRequest.value =
                            DialogRequest.ForTransition(fromState, toState, transition.read)
                    },
                    onRemoveTransition = { transition ->
                        machine.removeTransition(transition)
                        viewModel.markDirty()
                        recomposeKey.intValue++
                    },
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                machine.TreeView(
                    recomposeKey = recomposeKey.intValue,
                    inspectedNodeId = viewModel.inspectedNodeId,
                    onSelectNode = { nodeId ->
                        viewModel.inspectNode(machine, nodeId)
                        if (machine is PushdownMachine) {
                            machine.selectNode(nodeId)
                        }
                        recomposeKey.intValue++
                    },
                )
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    FormalDefinitionView(machine.getFormalDefinition())
                }
            }
        }
    }
}

@Composable
private fun NewMachineWindow(onImport: (Machine?) -> Unit) {
    var machineName by remember { mutableStateOf("") }
    var machineType by remember { mutableStateOf<MachineType?>(null) }

    DefaultDialog(
        onDismissRequest = { onImport(null) },
        buttons = {
            CancelButton(onClick = { onImport(null) })
            CreateButton(
                onClick = {
                    val trimmedName = machineName.trim()
                    when (machineType) {
                        MachineType.FINITE -> onImport(FiniteMachine(name = trimmedName))
                        MachineType.PUSHDOWN -> onImport(PushdownMachine(name = trimmedName))
                        null -> {}
                    }
                },
                enabled = machineType != null && machineName.isNotBlank(),
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemSpecificationIcon(
                icon = R.drawable.finite_automata,
                text = stringResource(R.string.finite_automaton),
                isActive = machineType == MachineType.FINITE,
            ) {
                machineType = MachineType.FINITE
            }
            ItemSpecificationIcon(
                icon = R.drawable.pushdown_automata,
                text = stringResource(R.string.pushdown_automaton),
                isActive = machineType == MachineType.PUSHDOWN,
            ) {
                machineType = MachineType.PUSHDOWN
            }
        }

        DefaultTextField(
            value = machineName,
            onValueChange = { machineName = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.machine_name),
            suffix = ".jff",
        )
    }
}
