package com.sfag.automata.ui

import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.R
import com.sfag.automata.data.Jff
import com.sfag.automata.data.exportToJff
import com.sfag.automata.data.toMachine
import com.sfag.automata.domain.common.checkDeterminism
import com.sfag.automata.domain.common.getFormalDefinition
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.ui.common.FormalDefinitionView
import com.sfag.automata.ui.edit.StateList
import com.sfag.automata.ui.edit.TransitionList
import com.sfag.automata.ui.machine.DialogRequest
import com.sfag.automata.ui.machine.MachineView
import com.sfag.automata.ui.machine.TransitionAnimation
import com.sfag.automata.ui.machine.computeTransitionPaths
import com.sfag.automata.ui.tree.TreeView
import com.sfag.main.config.EXTRA_OPEN_FILE_PICKER
import com.sfag.main.config.JFF_OPEN_MIME_TYPES
import com.sfag.main.config.JFF_SAVE_MIME_TYPE
import com.sfag.main.data.JffUtils
import com.sfag.main.ui.component.DefaultButton
import com.sfag.main.ui.component.DefaultDialog
import com.sfag.main.ui.component.DefaultIconButton
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.ItemSpecificationIcon
import kotlinx.coroutines.launch

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
    val activity = LocalActivity.current as? AppCompatActivity ?: return
    val viewModel: AutomataViewModel = hiltViewModel(activity)
    val noInitialStateMsg = stringResource(R.string.no_initial_state)
    val importErrorMsg = stringResource(R.string.file_import_error)
    val scope = rememberCoroutineScope()

    val initImportLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    val fileName = activity.contentResolver
                        .query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0)
                                ?.removeSuffix(".jff") else null
                        } ?: "untitled"
                    activity.contentResolver.openInputStream(it)?.use { stream ->
                        val jff = Jff.parse(stream)
                        viewModel.setCurrentMachine(jff.toMachine(fileName), jff.positions)
                    }
                } catch (e: Exception) {
                    Log.e("AutomataScreen", "Failed to import file", e)
                    scope.launch { snackbarHostState.showSnackbar(importErrorMsg) }
                    navBack()
                }
            }
        }

    val machine = viewModel.currentMachine
    if (machine == null) {
        val importMode = activity.intent?.getBooleanExtra(EXTRA_OPEN_FILE_PICKER, false) ?: false
        if (importMode) {
            LaunchedEffect(Unit) { initImportLauncher.launch(JFF_OPEN_MIME_TYPES) }
        } else {
            LaunchedEffect(Unit) { navBack() }
        }
        return
    }

    key(machine) {
        val recomposeKey = remember { mutableIntStateOf(0) }

        // Ensure initial state is highlighted and derivation tree is ready on screen entry
        LaunchedEffect(machine) {
            machine.setInitialStateAsCurrent()
            recomposeKey.intValue++
        }

        val currentMode = remember { mutableStateOf(Mode.SIMULATOR) }
        var showUnsavedDialog by remember { mutableStateOf(viewModel.pendingExampleUri != null) }
        var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
        var isBackAction by remember { mutableStateOf(false) }
        var simulationOutcome by remember { mutableStateOf<SimulationOutcome?>(null) }
        val animationOverlay = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
        val dialogRequest = remember { mutableStateOf<DialogRequest?>(null) }
        var snackbarMsg by remember { mutableStateOf<String?>(null) }
        var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }
        val exportErrorMsg = stringResource(R.string.file_export_error)

        val exportLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument(JFF_SAVE_MIME_TYPE),
            ) { uri ->
                try {
                    uri?.let {
                        activity.contentResolver.openOutputStream(it)?.use { stream ->
                            stream.write(
                                machine
                                    .exportToJff(viewModel.getPositions())
                                    .toByteArray(Charsets.UTF_8),
                            )
                        }
                        activity.contentResolver.query(
                            it,
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )
                            ?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val fileName = cursor.getString(0) ?: return@use
                                    machine.name = fileName.removeSuffix(".jff")
                                    recomposeKey.intValue++
                                }
                            }
                        viewModel.markSaved()
                    }
                } catch (e: Exception) {
                    Log.e("AutomataScreen", "Failed to export file", e)
                    snackbarMsg = exportErrorMsg
                }
            }

        val importLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    try {
                        val fileName = activity.contentResolver
                            .query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { cursor ->
                                if (cursor.moveToFirst()) cursor.getString(0)
                                    ?.removeSuffix(".jff") else null
                            } ?: "untitled"
                        activity.contentResolver.openInputStream(it)?.use { stream ->
                            val jff = Jff.parse(stream)
                            viewModel.setCurrentMachine(jff.toMachine(fileName), jff.positions)
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
                        showUnsavedDialog = true
                        isBackAction = true
                    } else {
                        viewModel.autoSave(machine)
                        navBack()
                    }
                }

                Mode.SIMULATION_STEP -> {}
                Mode.INPUT_EDITOR -> {
                    machine.setInitialStateAsCurrent()
                    viewModel.autoSave(machine)
                    currentMode.value = Mode.SIMULATOR
                }

                Mode.MACHINE_EDITOR -> {
                    machine.setInitialStateAsCurrent()
                    recomposeKey.intValue++
                    viewModel.autoSave(machine)
                    currentMode.value = Mode.SIMULATOR
                }
            }
        }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let {
                snackbarHostState.showSnackbar(it)
                snackbarMsg = null
            }
        }

        val canvasScrollBlocker =
            remember {
                object : NestedScrollConnection {
                    // Consume vertical scroll only; horizontal passes to tape-bar LazyRows
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset = Offset(0f, available.y)
                }
            }

        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
            if (currentMode.value != Mode.INPUT_EDITOR) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
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
                                    text = stringResource(R.string.share_file),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    JffUtils.shareFile(
                                        context = activity,
                                        jffContent =
                                            machine.exportToJff(viewModel.getPositions()),
                                        filename = machine.name,
                                    )
                                }

                                DefaultButton(
                                    text = stringResource(R.string.save_file),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    exportLauncher.launch(
                                        "${machine.name}.jff",
                                    )
                                }

                                DefaultButton(
                                    text = stringResource(R.string.import_file),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (viewModel.hasUnsavedChanges) {
                                        showUnsavedDialog = true
                                        isBackAction = false
                                        pendingAction =
                                            { importLauncher.launch(JFF_OPEN_MIME_TYPES) }
                                    } else {
                                        importLauncher.launch(JFF_OPEN_MIME_TYPES)
                                    }
                                }

                                DefaultButton(
                                    text = stringResource(R.string.create_new),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (viewModel.hasUnsavedChanges) {
                                        showUnsavedDialog = true
                                        isBackAction = false
                                        pendingAction = { activeDialog = ActiveDialog.NewMachine }
                                    } else {
                                        activeDialog = ActiveDialog.NewMachine
                                    }
                                }
                            }

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .nestedScroll(canvasScrollBlocker)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                            ) {
                                machine.MachineView(
                                    isEditing =
                                        currentMode.value == Mode.MACHINE_EDITOR,
                                    recomposeKey = recomposeKey.intValue,
                                    animationOverlay = animationOverlay.value,
                                    dialogRequest = dialogRequest,
                                    simulationOutcome = simulationOutcome,
                                    onEdit = {
                                        if (
                                            currentMode.value != Mode.SIMULATION_STEP
                                        ) {
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
                                    icon = R.drawable.edit,
                                    modifier = Modifier.weight(1f),
                                    isActive =
                                        currentMode.value == Mode.MACHINE_EDITOR,
                                ) {
                                    if (currentMode.value == Mode.SIMULATION_STEP) {
                                        return@DefaultIconButton
                                    }
                                    if (currentMode.value == Mode.MACHINE_EDITOR) {
                                        machine.setInitialStateAsCurrent()
                                        recomposeKey.intValue++
                                        currentMode.value = Mode.SIMULATOR
                                    } else {
                                        currentMode.value = Mode.MACHINE_EDITOR
                                    }
                                }
                                DefaultIconButton(
                                    icon =
                                        if (currentMode.value == Mode.MACHINE_EDITOR) {
                                            R.drawable.center_focus
                                        } else {
                                            R.drawable.replay
                                        },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (currentMode.value == Mode.MACHINE_EDITOR) {
                                        viewModel.machineAutoCenter = true
                                        recomposeKey.intValue++
                                    } else {
                                        simulationOutcome = null
                                        animationOverlay.value = null
                                        machine.setInitialStateAsCurrent()
                                        recomposeKey.intValue++
                                        currentMode.value = Mode.SIMULATOR
                                    }
                                }
                                DefaultIconButton(
                                    icon = R.drawable.skip_next,
                                    modifier = Modifier.weight(1f),
                                ) {
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
                                            simulationOutcome = simulation.outcome
                                            recomposeKey.intValue++
                                            snackbarMsg =
                                                when (simulation.outcome) {
                                                    SimulationOutcome.ACCEPTED -> {
                                                        val stateNames =
                                                            machine.states
                                                                .filter { it.isCurrent && it.final }
                                                                .joinToString(", ") { it.name }
                                                        activity.getString(
                                                            R.string.accepted_in_states,
                                                            stateNames,
                                                        )
                                                    }

                                                    SimulationOutcome.REJECTED -> {
                                                        val stateNames =
                                                            machine.states
                                                                .filter { it.isCurrent }
                                                                .joinToString(", ") { it.name }
                                                        activity.getString(
                                                            R.string.rejected_in_states,
                                                            stateNames,
                                                        )
                                                    }

                                                    SimulationOutcome.ACTIVE,
                                                    SimulationOutcome.DEAD,
                                                        -> null
                                                }
                                        }

                                        is Simulation.Step -> {
                                            currentMode.value = Mode.SIMULATION_STEP
                                            val capturedPositions = viewModel.statePositions.toMap()
                                            animationOverlay.value = {
                                                val animDensity = LocalDensity.current
                                                val transitionPaths =
                                                    machine.computeTransitionPaths(
                                                        capturedPositions,
                                                        animDensity.density,
                                                    )
                                                machine.TransitionAnimation(
                                                    transitionRefs = simulation.transitionRefs,
                                                    transitionPaths = transitionPaths,
                                                    offsetXCanvas = viewModel.offsetXCanvas,
                                                    offsetYCanvas = viewModel.offsetYCanvas,
                                                    onAnimationsEnd = {
                                                        simulation.onAllComplete()
                                                        animationOverlay.value = null
                                                        recomposeKey.intValue++
                                                        currentMode.value =
                                                            Mode.SIMULATOR
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
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
                machine.InputScreen(
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

            when (val dialog = activeDialog) {
                is ActiveDialog.NewMachine ->
                    NewMachineWindow { newMachine ->
                        activeDialog = null
                        newMachine?.let {
                            viewModel.setCurrentMachine(it)
                        }
                    }

                null -> {}
            }
            if (showUnsavedDialog) {
                val proceed = {
                    showUnsavedDialog = false
                    val exampleUri = viewModel.pendingExampleUri
                    if (exampleUri != null) {
                        val exampleName = viewModel.pendingExampleName ?: "untitled"
                        viewModel.pendingExampleUri = null
                        viewModel.pendingExampleName = null
                        try {
                            val jff = activity.assets.open(exampleUri).use { Jff.parse(it) }
                            viewModel.setCurrentMachine(jff.toMachine(exampleName), jff.positions)
                        } catch (e: Exception) {
                            Log.e("AutomataScreen", "Failed to load example: $exampleUri", e)
                        }
                    } else if (isBackAction) {
                        navBack()
                    } else {
                        pendingAction?.invoke()
                    }
                    pendingAction = null
                }
                DefaultDialog(
                    title = stringResource(R.string.unsaved_changes),
                    cancelLabel = stringResource(R.string.discard_button),
                    onDismissRequest = {
                        showUnsavedDialog = false
                        pendingAction = null
                    },
                    onDismiss = {
                        proceed()
                    },
                    onConfirm = {
                        viewModel.autoSave(machine)
                        proceed()
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
                            DialogRequest.ForTransition(fromState, toState, transition.name)
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
                val showsTree = machine.checkDeterminism() == false
                if (showsTree) {
                    machine.TreeView(
                        recomposeKey = recomposeKey.intValue,
                        onSelectNode =
                            if (machine is PushdownMachine) {
                                { nodeId ->
                                    machine.selectNode(nodeId)
                                    recomposeKey.intValue++
                                }
                            } else {
                                null
                            },
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    FormalDefinitionView(machine.getFormalDefinition())
                }
            }
        }
    }
}

private enum class NewMachineChoice {
    FINITE,
    PUSHDOWN,
}

@Composable
private fun NewMachineWindow(onImport: (Machine?) -> Unit) {
    var machineChoice by remember { mutableStateOf<NewMachineChoice?>(null) }
    val defaultName = stringResource(R.string.untitled_name)
    var machineName by remember { mutableStateOf(defaultName) }

    DefaultDialog(
        title = null,
        confirmLabel = stringResource(R.string.create_button),
        enabled = machineChoice != null,
        onDismiss = { onImport(null) },
        onConfirm = {
            when (machineChoice) {
                NewMachineChoice.FINITE -> onImport(FiniteMachine(name = machineName))
                NewMachineChoice.PUSHDOWN -> onImport(PushdownMachine(name = machineName))
                null -> {}
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemSpecificationIcon(
                icon = R.drawable.finite_automata,
                text = stringResource(R.string.finite_automaton),
                isActive = machineChoice == NewMachineChoice.FINITE,
            ) {
                machineChoice = NewMachineChoice.FINITE
            }
            ItemSpecificationIcon(
                icon = R.drawable.pushdown_automata,
                text = stringResource(R.string.pushdown_automaton),
                isActive = machineChoice == NewMachineChoice.PUSHDOWN,
            ) {
                machineChoice = NewMachineChoice.PUSHDOWN
            }
        }

        DefaultTextField(
            label = stringResource(R.string.machine_name),
            value = machineName,
            suffix = ".jff",
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { machineName = it },
        )
    }
}
