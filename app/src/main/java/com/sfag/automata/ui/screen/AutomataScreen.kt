package com.sfag.automata.ui.screen

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.automata.domain.model.machine.FiniteMachine
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.TuringTransition
import com.sfag.automata.ui.viewmodel.AutomataViewModel
import androidx.compose.ui.platform.LocalDensity
import com.sfag.automata.ui.component.AutomataView
import com.sfag.automata.ui.component.EditActions
import com.sfag.automata.ui.component.computePaths
import com.sfag.automata.ui.component.edit.EditingInput
import com.sfag.automata.ui.component.edit.EditingMachineBottom
import com.sfag.automata.ui.component.simulation.MultipleAnimationsOfTransition
import com.sfag.automata.ui.component.visualization.DerivationTree
import com.sfag.automata.ui.component.visualization.MathFormatView
import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.ui.model.AutomataScreenStates
import com.sfag.R
import com.sfag.automata.data.JffParser
import com.sfag.automata.ui.component.simulation.MACHINE_DIAGRAM_HEIGHT
import com.sfag.automata.ui.component.simulation.TAPE_BAR_HEIGHT
import com.sfag.automata.ui.component.widget.ItemSpecificationIcon
import com.sfag.shared.ui.component.DefaultDialogWindow
import com.sfag.automata.ui.component.widget.DefaultTextField
import androidx.compose.ui.res.stringResource
import com.sfag.shared.ui.component.DefaultButton
import com.sfag.shared.ui.component.DefaultIconButton

@Composable
fun AutomataScreen(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }, navBack: () -> Unit) {
    val viewModel: AutomataViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
    val automata = viewModel.currentMachine ?: run { navBack(); return }

    val recompose = remember {
        mutableIntStateOf(0)
    }

    // On screen entry: ensure initial state is highlighted and derivation tree is ready.
    // Fixes inconsistency between newly-created machines (isCurrent set via addNewState)
    // and machines loaded from file (isCurrent defaults to false in JffParser).
    LaunchedEffect(automata) {
        automata.setInitialStateAsCurrent()
        recompose.intValue++
    }

    val animation = remember {
        mutableIntStateOf(0)
    }
    val currentScreenState = remember {
        mutableStateOf(AutomataScreenStates.SIMULATING)
    }
    var isLockedAnimation by remember { mutableStateOf(true) }
    var simulationEndResult by remember { mutableStateOf<Boolean?>(null) }
    val animOverlay = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val editActions = remember { EditActions() }
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showNewMachineDialog by remember { mutableStateOf(false) }
    var pendingName by remember { mutableStateOf("") }

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(automata.exportToJFF(viewModel.getPositionsAsVec2()).toByteArray(Charsets.UTF_8))
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
                viewModel.autoSave(automata)
                viewModel.setCurrentMachine(machine, parseResult.positions)
            }
        }
    }

    BackHandler {
        when (currentScreenState.value) {
            AutomataScreenStates.SIMULATING -> {
                viewModel.autoSave(automata)
                navBack()
            }

            AutomataScreenStates.SIMULATION_RUN -> {
                // Ignore back during animation
            }

            AutomataScreenStates.EDITING_INPUT -> {
                automata.setInitialStateAsCurrent()
                viewModel.autoSave(automata)
                currentScreenState.value = AutomataScreenStates.SIMULATING
            }

            AutomataScreenStates.EDITING_MACHINE -> {
                viewModel.autoSave(automata)
                currentScreenState.value = AutomataScreenStates.SIMULATING
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
        if (currentScreenState.value != AutomataScreenStates.EDITING_INPUT) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DefaultButton(
                            text = stringResource(R.string.new_label),
                            modifier = Modifier.weight(1f)
                        ) {
                            showNewMachineDialog = true
                        }

                        DefaultButton(
                            text = stringResource(R.string.import_label),
                            modifier = Modifier.weight(1f)
                        ) {
                            importLauncher.launch(arrayOf("application/octet-stream", "text/xml"))
                        }

                        DefaultButton(
                            text = stringResource(R.string.export_label),
                            modifier = Modifier.weight(1f)
                        ) {
                            exportLauncher.launch("${automata.name.ifEmpty { "untitled" }}.jff")
                        }

                        DefaultButton(
                            text = stringResource(R.string.share),
                            modifier = Modifier.weight(1f)
                        ) {
                            JffParser.shareJffFile(
                                context = context,
                                jffContent = automata.exportToJFF(viewModel.getPositionsAsVec2()),
                                filename = automata.name
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                TAPE_BAR_HEIGHT + MACHINE_DIAGRAM_HEIGHT +
                                    if (automata.machineType == MachineType.Pushdown &&
                                        currentScreenState.value != AutomataScreenStates.EDITING_MACHINE
                                    ) TAPE_BAR_HEIGHT else 0.dp
                            )
                            .nestedScroll(diagramScrollBlocker)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        // Preserve original trigger structure: key resets the scope each step,
                        // isLockedAnimation prevents re-calling calculateNextStep on recompose.
                        if (currentScreenState.value == AutomataScreenStates.SIMULATION_RUN) {
                            key(animation.intValue) {
                                if (!isLockedAnimation) {
                                    when (val result = viewModel.calculateNextStep()) {
                                        is SimulationResult.Ended -> {
                                            isLockedAnimation = true
                                            simulationEndResult = result.accepted
                                            recompose.intValue++
                                            currentScreenState.value = AutomataScreenStates.SIMULATING
                                            val message = when (result.accepted) {
                                                true -> {
                                                    val acceptingStates = automata.states
                                                        .filter { it.isCurrent && it.final }
                                                        .joinToString(", ") { it.name }
                                                    context.resources.getString(R.string.accepted_in_states, acceptingStates)
                                                }
                                                false -> {
                                                    val currentStatesStr = automata.states
                                                        .filter { it.isCurrent }
                                                        .joinToString(", ") { it.name }
                                                    context.resources.getString(R.string.rejected_in_states, currentStatesStr)
                                                }
                                                null -> null
                                            }
                                            message?.let { pendingMessage = it }
                                        }
                                        is SimulationResult.Step -> {
                                            // Lock before setting animOverlay — otherwise the
                                            // state write triggers a recompose where
                                            // !isLockedAnimation is still true and
                                            // calculateNextStep() runs a second time.
                                            isLockedAnimation = true
                                            val capturedPositions = viewModel.statePositions.toMap()
                                            animOverlay.value = {
                                                val animDensity = LocalDensity.current
                                                val renderData = automata.computePaths(capturedPositions, animDensity)
                                                automata.MultipleAnimationsOfTransition(
                                                    transitions = result.transitions,
                                                    renderData = renderData,
                                                    offsetXGraph = viewModel.offsetXGraph,
                                                    offsetYGraph = viewModel.offsetYGraph,
                                                    onAllAnimationsEnd = {
                                                        result.onAllComplete()
                                                        animOverlay.value = null
                                                        recompose.intValue++
                                                        currentScreenState.value = AutomataScreenStates.SIMULATING
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        automata.AutomataView(
                            isEditing = currentScreenState.value == AutomataScreenStates.EDITING_MACHINE,
                            recomposeKey = recompose.intValue,
                            onEditInputClick = {
                                simulationEndResult = null
                                currentScreenState.value = AutomataScreenStates.EDITING_INPUT
                            },
                            increaseRecomposeValue = { recompose.intValue++ },
                            animationOverlay = animOverlay.value,
                            editActions = editActions,
                            onNameClick = {
                                pendingName = automata.name.ifEmpty { "" }
                                showRenameDialog = true
                            },
                            simulationEndResult = simulationEndResult
                        )
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DefaultIconButton(
                            icon = R.drawable.edit,
                            modifier = Modifier.weight(1f),
                            isActive = currentScreenState.value == AutomataScreenStates.EDITING_MACHINE
                        ) {
                            if (currentScreenState.value == AutomataScreenStates.EDITING_MACHINE) {
                                currentScreenState.value = AutomataScreenStates.SIMULATING
                            } else {
                                currentScreenState.value = AutomataScreenStates.EDITING_MACHINE
                            }
                        }
                        DefaultIconButton(icon = R.drawable.replay, modifier = Modifier.weight(1f)) {
                            isLockedAnimation = true
                            simulationEndResult = null
                            animOverlay.value = null
                            // Restore full input from snapshot (automata.input may be
                            // partially consumed; fullInputSnapshot holds the original).
                            automata.input.clear()
                            automata.input.append(automata.fullInputSnapshot)
                            automata.imuInput = StringBuilder(automata.fullInputSnapshot)
                            // Reset state highlighting + derivation tree + machine internals
                            // (stack / tape / currentStates via resetMachineState override).
                            automata.setInitialStateAsCurrent()
                            viewModel.needsAutoFit = true
                            recompose.intValue++
                            currentScreenState.value = AutomataScreenStates.SIMULATING
                        }
                        DefaultIconButton(icon = R.drawable.skip_next, modifier = Modifier.weight(1f)) {
                            if (currentScreenState.value != AutomataScreenStates.SIMULATING &&
                                currentScreenState.value != AutomataScreenStates.SIMULATION_RUN
                            ) {
                                currentScreenState.value = AutomataScreenStates.SIMULATING
                            }
                            currentScreenState.value = AutomataScreenStates.SIMULATION_RUN
                            if (isLockedAnimation) {
                                isLockedAnimation = false
                                animation.intValue++
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    BottomScreenPart(currentScreenState, automata, viewModel, bottomRecompose = recompose, editActions = editActions)

                    Spacer(modifier = Modifier.size(12.dp))
                }
            }
        }

        // Full-screen input editor overlay - hides all other controls when active
        if (currentScreenState.value == AutomataScreenStates.EDITING_INPUT) {
            automata.EditingInput(
                onConfirm = {
                    viewModel.autoSave(automata)
                    currentScreenState.value = AutomataScreenStates.SIMULATING
                },
                onDiscard = {
                    viewModel.autoSave(automata)
                    currentScreenState.value = AutomataScreenStates.SIMULATING
                }
            )
        }

        if (showRenameDialog) {
            DefaultDialogWindow(
                title = null,
                conditionToEnable = pendingName.isNotBlank(),
                onDismiss = { showRenameDialog = false },
                onConfirm = {
                    automata.name = pendingName
                    viewModel.autoSave(automata)
                    showRenameDialog = false
                }
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                DefaultTextField(
                    hint = stringResource(R.string.automata_name),
                    value = pendingName,
                    suffix = stringResource(R.string.jff_suffix),
                    modifier = Modifier.fillMaxWidth(),
                    onTextChange = { pendingName = it }
                ) { pendingName.isNotBlank() }
            }
        }

        if (showNewMachineDialog) {
            NewMachineWindow { newMachine ->
                showNewMachineDialog = false
                newMachine?.let {
                    viewModel.autoSave(automata)
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
    currentScreenState: MutableState<AutomataScreenStates>,
    automata: Machine,
    viewModel: AutomataViewModel,
    bottomRecompose: MutableIntState,
    editActions: EditActions
) {
    when (currentScreenState.value) {
        AutomataScreenStates.SIMULATING,
        AutomataScreenStates.SIMULATION_RUN -> {
            automata.DerivationTree(recomposeKey = bottomRecompose.intValue)
            if (currentScreenState.value == AutomataScreenStates.SIMULATING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    MathFormatView(automata.getMathFormatData())
                }
            }
        }

        AutomataScreenStates.EDITING_MACHINE -> {
            automata.EditingMachineBottom(
                recompose = bottomRecompose,
                onStateClick = editActions.openState,
                onTransitionClick = editActions.openTransition,
                onDeleteState = { state ->
                    viewModel.statePositions.remove(state.index)
                    automata.deleteState(state)
                    bottomRecompose.intValue++
                },
                onDeleteTransition = { transition ->
                    automata.deleteTransition(transition)
                    bottomRecompose.intValue++
                }
            )
        }

        else -> {}
    }
}

@Composable
private fun NewMachineWindow(finished: (Machine?) -> Unit) {
    var type by remember {
        mutableStateOf<MachineType?>(null)
    }

    val createLabel = stringResource(R.string.create)
    val pushdownLabel = stringResource(R.string.pushdown)
    val finiteLabel = stringResource(R.string.finite)

    DefaultDialogWindow(
        title = null,
        confirmLabel = createLabel,
        conditionToEnable = type != null,
        onDismiss = {
            finished(null)
        },
        onConfirm = {
            when (type) {
                MachineType.Finite -> finished(FiniteMachine())
                MachineType.Pushdown -> finished(PushDownMachine())
                else -> {}
            }
        }) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemSpecificationIcon(
                icon = R.drawable.pushdown_automata,
                text = pushdownLabel,
                isActive = type?.equals(MachineType.Pushdown) ?: false
            ) {
                type = MachineType.Pushdown
            }

            ItemSpecificationIcon(
                icon = R.drawable.finite_automata,
                text = finiteLabel,
                isActive = type?.equals(MachineType.Finite) ?: false
            ) {
                type = MachineType.Finite
            }
        }
    }
}
