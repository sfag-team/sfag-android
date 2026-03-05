package com.sfag.automata.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.sfag.R
import com.sfag.automata.AUTOMATA_CANVAS_HEIGHT
import com.sfag.automata.TAPE_BAR_HEIGHT
import com.sfag.automata.data.JffParser
import com.sfag.automata.data.toMachine
import com.sfag.automata.model.machine.FiniteMachine
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.MachineType
import com.sfag.automata.model.machine.PushdownMachine
import com.sfag.automata.model.machine.isDeterministic
import com.sfag.automata.model.simulation.SimulationOutcome
import com.sfag.automata.model.simulation.SimulationStep
import com.sfag.automata.ui.diagram.AutomataView
import com.sfag.automata.ui.diagram.EditActions
import com.sfag.automata.ui.diagram.MathFormatView
import com.sfag.automata.ui.diagram.MultipleAnimationsOfTransition
import com.sfag.automata.ui.diagram.computePaths
import com.sfag.automata.ui.editor.EditingInput
import com.sfag.automata.ui.editor.EditingMachineBottom
import com.sfag.automata.ui.tree.DerivationTree
import com.sfag.shared.JFF_OPEN_MIME_TYPES
import com.sfag.shared.JFF_SAVE_MIME_TYPES
import com.sfag.shared.ui.component.DefaultButton
import com.sfag.shared.ui.component.DefaultDialogWindow
import com.sfag.shared.ui.component.DefaultIconButton
import com.sfag.shared.ui.component.DefaultTextField
import com.sfag.shared.ui.component.ItemSpecificationIcon
import com.sfag.shared.util.JffUtils

private enum class ScreenState {
    SIMULATING,
    SIMULATION_RUN,
    EDITING_INPUT,
    EDITING_MACHINE,
}

private enum class ActiveDialog { RENAME, NEW_MACHINE }

@Composable
fun AutomataScreen(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }, navBack: () -> Unit) {
    val viewModel: AutomataViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
    val context = LocalContext.current
    val noInitialStateMessage = stringResource(R.string.no_initial_state)

    val initImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { it ->
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            content?.let { jffXml ->
                val parseResult = JffParser.parseJffWithType(jffXml)
                val imported = parseResult.toMachine("untitled")
                viewModel.setCurrentMachine(imported, parseResult.positions)
            }
        }
    }

    val machine = viewModel.currentMachine
    if (machine == null) {
        NewMachineWindow(
            onImport = {
                initImportLauncher.launch(JFF_OPEN_MIME_TYPES)
            }
        ) { newMachine ->
            if (newMachine != null) {
                viewModel.setCurrentMachine(newMachine)
            } else {
                navBack()
            }
        }
        return
    }

    val recompose = remember {
        mutableIntStateOf(0)
    }

    // On screen entry: ensure initial state is highlighted and derivation tree is ready.
    // Fixes inconsistency between newly-created machines (isCurrent set via addNewState)
    // and machines loaded from file (isCurrent defaults to false in JffParser).
    LaunchedEffect(machine) {
        machine.setInitialStateAsCurrent()
        recompose.intValue++
    }

    val animation = remember {
        mutableIntStateOf(0)
    }
    val currentScreenState = remember {
        mutableStateOf(ScreenState.SIMULATING)
    }
    var isLockedAnimation by remember { mutableStateOf(true) }
    var simulationOutcome by remember { mutableStateOf<SimulationOutcome?>(null) }
    val animOverlay = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val editActions = remember { EditActions() }
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }
    var pendingName by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(JFF_SAVE_MIME_TYPES)
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(machine.exportToJFF(viewModel.getPositionsAsVec2()).toByteArray(Charsets.UTF_8))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { it ->
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            content?.let { jffXml ->
                val parseResult = JffParser.parseJffWithType(jffXml)
                val imported = parseResult.toMachine("untitled")
                viewModel.autoSave(machine)
                viewModel.setCurrentMachine(imported, parseResult.positions)
            }
        }
    }

    BackHandler {
        when (currentScreenState.value) {
            ScreenState.SIMULATING -> {
                viewModel.autoSave(machine)
                navBack()
            }

            ScreenState.SIMULATION_RUN -> {
                // Ignore back during animation
            }

            ScreenState.EDITING_INPUT -> {
                machine.setInitialStateAsCurrent()
                viewModel.autoSave(machine)
                currentScreenState.value = ScreenState.SIMULATING
            }

            ScreenState.EDITING_MACHINE -> {
                machine.setInitialStateAsCurrent()
                recompose.intValue++
                viewModel.autoSave(machine)
                currentScreenState.value = ScreenState.SIMULATING
            }
        }
    }

    LaunchedEffect(pendingMessage) {
        pendingMessage?.let {
            snackbarHostState.showSnackbar(it)
            pendingMessage = null
        }
    }

    val diagramScrollBlocker = remember {
        object : NestedScrollConnection {
            // Only consume vertical scroll; horizontal is used by tape-bar LazyRows
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                Offset(0f, available.y)
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        if (currentScreenState.value != ScreenState.EDITING_INPUT) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DefaultButton(
                            text = stringResource(R.string.share_label),
                            modifier = Modifier.weight(1f)
                        ) {
                            JffUtils.shareFile(
                                context = context,
                                jffContent = machine.exportToJFF(viewModel.getPositionsAsVec2()),
                                filename = machine.name
                            )
                        }

                        DefaultButton(
                            text = stringResource(R.string.export_label),
                            modifier = Modifier.weight(1f)
                        ) {
                            exportLauncher.launch("${machine.name.ifEmpty { "untitled" }}.jff")
                        }

                        DefaultButton(
                            text = stringResource(R.string.import_label),
                            modifier = Modifier.weight(1f)
                        ) {
                            importLauncher.launch(JFF_OPEN_MIME_TYPES)
                        }

                        DefaultButton(
                            text = stringResource(R.string.new_label),
                            modifier = Modifier.weight(1f)
                        ) {
                            activeDialog = ActiveDialog.NEW_MACHINE
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                TAPE_BAR_HEIGHT + AUTOMATA_CANVAS_HEIGHT +
                                    if (machine.machineType == MachineType.Pushdown &&
                                        currentScreenState.value != ScreenState.EDITING_MACHINE
                                    ) TAPE_BAR_HEIGHT else 0.dp
                            )
                            .nestedScroll(diagramScrollBlocker)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        // Preserve original trigger structure: key resets the scope each step,
                        // isLockedAnimation prevents re-calling calculateNextStep on recompose.
                        if (currentScreenState.value == ScreenState.SIMULATION_RUN) {
                            key(animation.intValue) {
                                if (!isLockedAnimation) {
                                    when (val result = viewModel.calculateNextStep()) {
                                        is SimulationStep.Ended -> {
                                            isLockedAnimation = true
                                            simulationOutcome = result.outcome
                                            recompose.intValue++
                                            currentScreenState.value = ScreenState.SIMULATING
                                            val message = when (result.outcome) {
                                                SimulationOutcome.ACCEPTED -> {
                                                    val acceptingStates = machine.states
                                                        .filter { it.isCurrent && it.final }
                                                        .joinToString(", ") { it.name }
                                                    stringResource(R.string.accepted_in_states, acceptingStates)
                                                }
                                                SimulationOutcome.REJECTED -> {
                                                    val currentStatesStr = machine.states
                                                        .filter { it.isCurrent }
                                                        .joinToString(", ") { it.name }
                                                    stringResource(R.string.rejected_in_states, currentStatesStr)
                                                }
                                                SimulationOutcome.ACTIVE -> null
                                            }
                                            message?.let { pendingMessage = it }
                                        }
                                        is SimulationStep.Step -> {
                                            // Lock before setting animOverlay - otherwise the
                                            // state write triggers a recompose where
                                            // !isLockedAnimation is still true and
                                            // calculateNextStep() runs a second time.
                                            isLockedAnimation = true
                                            val capturedPositions = viewModel.statePositions.toMap()
                                            animOverlay.value = {
                                                val animDensity = LocalDensity.current
                                                val renderData = machine.computePaths(capturedPositions, animDensity)
                                                machine.MultipleAnimationsOfTransition(
                                                    transitions = result.transitions,
                                                    renderData = renderData,
                                                    offsetXGraph = viewModel.offsetXGraph,
                                                    offsetYGraph = viewModel.offsetYGraph,
                                                    onAllAnimationsEnd = {
                                                        result.onAllComplete()
                                                        animOverlay.value = null
                                                        recompose.intValue++
                                                        currentScreenState.value = ScreenState.SIMULATING
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        machine.AutomataView(
                            isEditing = currentScreenState.value == ScreenState.EDITING_MACHINE,
                            recomposeKey = recompose.intValue,
                            onEditInputClick = {
                                simulationOutcome = null
                                currentScreenState.value = ScreenState.EDITING_INPUT
                            },
                            increaseRecomposeValue = { recompose.intValue++ },
                            animationOverlay = animOverlay.value,
                            editActions = editActions,
                            onNameClick = {
                                pendingName = machine.name.ifEmpty { "" }
                                activeDialog = ActiveDialog.RENAME
                            },
                            simulationOutcome = simulationOutcome
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DefaultIconButton(
                            icon = R.drawable.edit,
                            modifier = Modifier.weight(1f),
                            isActive = currentScreenState.value == ScreenState.EDITING_MACHINE
                        ) {
                            if (currentScreenState.value == ScreenState.EDITING_MACHINE) {
                                machine.setInitialStateAsCurrent()
                                recompose.intValue++
                                currentScreenState.value = ScreenState.SIMULATING
                            } else {
                                currentScreenState.value = ScreenState.EDITING_MACHINE
                            }
                        }
                        DefaultIconButton(icon = R.drawable.replay, modifier = Modifier.weight(1f)) {
                            isLockedAnimation = true
                            simulationOutcome = null
                            animOverlay.value = null
                            // Restore full input from snapshot (machine.input may be
                            // partially consumed; fullInputSnapshot holds the original).
                            machine.input.clear()
                            machine.input.append(machine.fullInputSnapshot)
                            machine.imuInput = StringBuilder(machine.fullInputSnapshot)
                            // Reset state highlighting + derivation tree + machine internals
                            // (stack / tape / currentStates via resetMachineState override).
                            machine.setInitialStateAsCurrent()
                            recompose.intValue++
                            currentScreenState.value = ScreenState.SIMULATING
                        }
                        DefaultIconButton(icon = R.drawable.skip_next, modifier = Modifier.weight(1f)) {
                            if (currentScreenState.value == ScreenState.SIMULATION_RUN) return@DefaultIconButton
                            if (machine.states.none { it.initial }) {
                                pendingMessage = noInitialStateMessage
                                return@DefaultIconButton
                            }
                            if (currentScreenState.value != ScreenState.SIMULATING) {
                                currentScreenState.value = ScreenState.SIMULATING
                            }
                            currentScreenState.value = ScreenState.SIMULATION_RUN
                            isLockedAnimation = false
                            animation.intValue++
                        }
                    }

                    BottomScreenPart(currentScreenState, machine, viewModel, bottomRecompose = recompose, editActions = editActions)
                    }
                }
            }
        }

        // Full-screen input editor overlay - hides all other controls when active
        if (currentScreenState.value == ScreenState.EDITING_INPUT) {
            machine.EditingInput(
                onConfirm = {
                    viewModel.autoSave(machine)
                    currentScreenState.value = ScreenState.SIMULATING
                },
                onDiscard = {
                    viewModel.autoSave(machine)
                    currentScreenState.value = ScreenState.SIMULATING
                }
            )
        }

        if (activeDialog == ActiveDialog.RENAME) {
            DefaultDialogWindow(
                title = null,
                conditionToEnable = pendingName.isNotBlank(),
                onDismiss = { activeDialog = null },
                onConfirm = {
                    machine.name = pendingName
                    viewModel.autoSave(machine)
                    activeDialog = null
                }
            ) {
                DefaultTextField(
                    hint = stringResource(R.string.machine_name),
                    value = pendingName,
                    suffix = stringResource(R.string.jff_suffix),
                    modifier = Modifier.fillMaxWidth(),
                    onTextChange = { pendingName = it }
                ) { pendingName.isNotBlank() }
            }
        }

        if (activeDialog == ActiveDialog.NEW_MACHINE) {
            NewMachineWindow { newMachine ->
                activeDialog = null
                newMachine?.let {
                    viewModel.autoSave(machine)
                    viewModel.setCurrentMachine(it)
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
    currentScreenState: MutableState<ScreenState>,
    machine: Machine,
    viewModel: AutomataViewModel,
    bottomRecompose: MutableIntState,
    editActions: EditActions
) {
    when (currentScreenState.value) {
        ScreenState.SIMULATING,
        ScreenState.SIMULATION_RUN -> {
            val showsTree = machine.isDeterministic() == false
            if (showsTree) {
                machine.DerivationTree(recomposeKey = bottomRecompose.intValue)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                MathFormatView(machine.getFormalDefinition())
            }
        }

        ScreenState.EDITING_MACHINE -> {
            machine.EditingMachineBottom(
                recompose = bottomRecompose,
                onStateClick = editActions.openState,
                onTransitionClick = editActions.openTransition,
                onDeleteState = { state ->
                    viewModel.statePositions.remove(state.index)
                    machine.deleteState(state)
                    bottomRecompose.intValue++
                },
                onDeleteTransition = { transition ->
                    machine.deleteTransition(transition)
                    bottomRecompose.intValue++
                }
            )
        }

        else -> {}
    }
}

@Composable
private fun NewMachineWindow(onImport: (() -> Unit)? = null, finished: (Machine?) -> Unit) {
    var type by remember { mutableStateOf<MachineType?>(null) }
    var name by remember { mutableStateOf("") }

    val pushdownLabel = stringResource(R.string.pushdown)
    val finiteLabel = stringResource(R.string.finite)

    val iconsAndTextField: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemSpecificationIcon(
                icon = R.drawable.finite_automata,
                text = finiteLabel,
                isActive = type?.equals(MachineType.Finite) ?: false
            ) {
                type = MachineType.Finite
            }

            ItemSpecificationIcon(
                icon = R.drawable.pushdown_automata,
                text = pushdownLabel,
                isActive = type?.equals(MachineType.Pushdown) ?: false
            ) {
                type = MachineType.Pushdown
            }
        }

        DefaultTextField(
            hint = stringResource(R.string.machine_name),
            value = name,
            suffix = stringResource(R.string.jff_suffix),
            modifier = Modifier.fillMaxWidth(),
            onTextChange = { name = it }
        )
    }

    val onCreateConfirm: () -> Unit = {
        when (type) {
            MachineType.Finite -> finished(FiniteMachine(name = name))
            MachineType.Pushdown -> finished(PushdownMachine(name = name))
            else -> {}
        }
    }

    if (onImport != null) {
        // Fresh install: Import + New buttons in content
        Dialog(onDismissRequest = { finished(null) }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    iconsAndTextField()

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DefaultButton(
                            text = stringResource(R.string.import_label),
                            modifier = Modifier.weight(1f),
                            height = 40
                        ) {
                            onImport()
                        }
                        DefaultButton(
                            text = stringResource(R.string.new_label),
                            modifier = Modifier.weight(1f),
                            height = 40,
                            conditionToEnable = type != null
                        ) {
                            onCreateConfirm()
                        }
                    }
                }
            }
        }
    } else {
        // Normal: Discard + New via DefaultDialogWindow
        DefaultDialogWindow(
            title = null,
            confirmLabel = stringResource(R.string.new_label),
            conditionToEnable = type != null,
            onDismiss = { finished(null) },
            onConfirm = onCreateConfirm
        ) {
            iconsAndTextField()
        }
    }
}
