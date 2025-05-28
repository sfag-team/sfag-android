package com.droidhen.formalautosim.core.entities.machines

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PathMeasure
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidhen.automata.R
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition
import com.droidhen.formalautosim.presentation.theme.light_blue
import com.droidhen.formalautosim.presentation.theme.perlamutr_white
import com.droidhen.formalautosim.presentation.theme.unable_views
import com.droidhen.formalautosim.utils.enums.EditMachineStates
import com.droidhen.formalautosim.utils.extensions.drawArrow
import views.DefaultFASDialogWindow
import views.DropDownSelector
import views.FASButton
import views.FASDefaultTextField
import views.FASImmutableTextField
import views.ItemSpecificationIcon
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Suppress("UNREACHABLE_CODE")
abstract class Machine(
    val name: String,
    var version: Int,
    val machineType: MachineType,
    val states: MutableList<State>,
    protected val transitions: MutableList<Transition>,
    protected var imuInput: StringBuilder = java.lang.StringBuilder(),
    val savedInputs: MutableList<StringBuilder>
) {

    private lateinit var density: Density
    private lateinit var context: Context
    private var globalCanvasPosition: LayoutCoordinates? = null
    var input = StringBuilder()
    var currentTreePosition = 1

    private var offsetXGraph = 0f
    private var offsetYGraph = 0f
    private var editMode = EditMachineStates.ADD_STATES
    abstract var currentState: Int?

    private var onBottomTransitionClicked: (Transition) -> Unit = {}
    private var onBottomStateClicked: (State) -> Unit = {}


    /**
     * return all paths for all exists transitions
     *
     * @return list of pairs path to path - the first path - path of arrow, second one - path for arrow head
     */
    private fun getAllPath(
        setTransition: (Transition) -> Unit,
    ): List<Pair<Path?, Path?>?> {
        val listOfPaths = arrayOfNulls<Pair<Path?, Path?>>(transitions.size)
        transitions.forEach { transition ->
            setTransition(transition)
            listOfPaths[transitions.indexOf(transition)] =
                getTransitionByPath(transition) { controlPoint ->
                    transition.controlPoint = controlPoint
                }
        }
        return listOfPaths.toList()
    }

    /**
     * create path for composing arrow on the screen
     *
     * @param transition - pair (evidence num of start state to ev. num of destination state)
     * @return pair Path to arrow, Path - to arrow head, in case that between states path doesn't exists - return pair null to null
     */
    private fun getTransitionByPath(
        transition: Transition? = null,
        startState: Offset? = null,
        endState: Offset? = null,
        primaryCurvature: Int = 100,
        setControlPoint: (Offset) -> Unit,
    ): Pair<Path?, Path?> {
        if (transition != null && !transitions.contains(transition)) return null to null


        val radius = states[0].radius
        val startPosition =
            if (transition == null) startState!! else getStateByIndex(transition.startState).position
        val endPosition =
            if (transition == null) endState!! else getStateByIndex(transition.endState).position

        val startPoint = startPosition.let { positionDP ->
            return@let with(density) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
        }

        val endPoint = endPosition.let { positionDP ->
            return@let with(density) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
        }
        return if (startPoint == endPoint) {
            val headPosition =
                Offset(
                    endPoint.first - 1.733f * radius,
                    endPoint.second - 0.628f * radius + 18f
                )
            setControlPoint(Offset(startPoint.first, startPoint.second - 2.8f * radius))
            return Path().apply {
                addOval(
                    Rect(
                        center = Offset(
                            x = startPoint.first,
                            y = startPoint.second - radius
                        ), radius = radius * 1.4f
                    )
                )
            } to getArrowHeadPath(headPosition, -0.9f, 0.436f, dirX = 1f, dirY = 0f)
        } else {
            val dx = endPoint.first - startPoint.first
            val dy = endPoint.second - startPoint.second
            val length = kotlin.math.sqrt(dx * dx + dy * dy)
            val curvature = primaryCurvature * length / 1000
            val dirX = dx / length
            val dirY = dy / length

            val startOffset = Offset(
                startPoint.first + radius * dirX,
                startPoint.second + radius * dirY
            )
            val endOffset = Offset(
                endPoint.first - radius * dirX,
                endPoint.second - radius * dirY
            )

            val controlPoint = Offset(
                (startPoint.first + endPoint.first) / 2 + curvature * dirY,
                (startPoint.second + endPoint.second) / 2 - curvature * dirX
            )
            setControlPoint(controlPoint)
            val angleRadians = atan((length / 2) / curvature)
            val cosTheta = cos(angleRadians)
            val sinTheta = sin(angleRadians)

            return Path().apply {
                moveTo(startOffset.x, startOffset.y)
                quadraticBezierTo(controlPoint.x, controlPoint.y, endOffset.x, endOffset.y)
            } to getArrowHeadPath(
                Offset(endOffset.x, endOffset.y),
                sinTheta,
                cosTheta,
                dirX = dirX,
                dirY = dirY
            )
        }
    }

    fun getArrowHeadPath(
        position: Offset,
        sin: Float,
        cos: Float,
        size: Float = 26f,
        dirX: Float,
        dirY: Float,
    ): Path {
        return Path().apply {
            val halfSize = size / 2

            fun rotateVector(dirX: Float, dirY: Float): Pair<Float, Float> {
                val rotatedX = dirX * sin - dirY * cos
                val rotatedY = dirX * cos + dirY * sin
                return rotatedX to rotatedY
            }

            val (rotatedDirX, rotatedDirY) = rotateVector(dirX, dirY)
            val (rotatedPerpDirX, rotatedPerpDirY) = rotateVector(-dirY, dirX)

            moveTo(position.x, position.y)

            lineTo(
                position.x - size * rotatedDirX - halfSize * rotatedPerpDirX,
                position.y - size * rotatedDirY - halfSize * rotatedPerpDirY
            )

            lineTo(
                position.x - size * rotatedDirX + halfSize * rotatedPerpDirX,
                position.y - size * rotatedDirY + halfSize * rotatedPerpDirY
            )
            close()
        }
    }

    /**
     * add new transition to machine
     *
     * @param transition - pair Int to Int where Int - evidence num of state
     */
    fun addTransition(transition: Transition) {
        if (transitions.contains(transition)) return
        if (states.any { transition.startState == it.index } && states.any { transition.endState == it.index }) {
            transitions.add(transition)
        }
    }

    /**
     * delete transition
     *
     * @param transition - pair Int to Int where Int - evidence num of state
     */
    fun deleteTransition(transition: Transition) {
        transitions.remove(transition)
    }

    /**
     * delete state
     *
     * @param state - state that should be deleted
     */
    fun deleteState(state: State) {
        states.remove(state)
        transitions.filter { it.startState == state.index || it.endState == state.index }
            .forEach { transitionToRemove ->
                transitions.remove(transitionToRemove)
            }
    }

    /**
     * simulate transition regarding current state and input
     */
    @Composable
    @SuppressLint("ComposableNaming")
    abstract fun calculateTransition(onAnimationEnd: (Boolean?) -> Unit)

    /**
     * convert machine to key - value String format for saving machine on relative database
     */
    abstract fun convertMachineToKeyValue(): List<Pair<String, String>>

    /**
     * @returns state with the same index, if it exists, else - returns null
     */
    fun getStateByIndex(index: Int): State = states.filter {
        it.index == index
    }[0]


    /**
     * draws machine with all states and transitions
     */
    @SuppressLint("ComposableNaming", "SuspiciousIndentation")
    @Composable
    fun SimulateMachine() {
        context = LocalContext.current
        density = LocalDensity.current
        var offsetX by remember {
            mutableFloatStateOf(offsetXGraph)
        }
        var offsetY by remember {
            mutableFloatStateOf(offsetYGraph)
        }

        val dragModifier = Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                offsetY += dragAmount.y
                offsetXGraph = offsetX
                offsetYGraph = offsetY
            }
        }

        Transitions(dragModifier = dragModifier, offsetY, offsetX, onTransitionClick = null)
        States(dragModifier = dragModifier, null, offsetY, offsetX, onStateClick = {})
        InputBar()
        if (machineType == MachineType.Pushdown) {
            BottomPushDownBar(this as PushDownMachine)
        }
    }


    @SuppressLint("DefaultLocale", "UnrememberedMutableState")
    @Composable
    fun EditingMachine( increaseRecomposeValue :() -> Unit) {
        var recomposition by remember {
            mutableIntStateOf(0)
        }
        var offsetX by remember {
            mutableFloatStateOf(offsetXGraph)
        }
        var offsetY by remember {
            mutableFloatStateOf(offsetYGraph)
        }
        var clickOffset by remember {
            mutableStateOf(Offset(0f, 0f))
        }
        var currentEditingState by remember {
            mutableStateOf(editMode)
        }
        var addStateWindowFocused by remember {
            mutableStateOf(false)
        }
        var addTransitionWindowFocused by remember {
            mutableStateOf(false)
        }
        var choosedStateForTransitionStart by remember {
            mutableStateOf<State?>(null)
        }

        var choosedStateForTransitionEnd by remember {
            mutableStateOf<State?>(null)
        }

        var choosedTransitionName by remember {
            mutableStateOf<String?>(null)
        }

        var push by remember {
            mutableStateOf<String?>(null)
        }

        var pop by remember {
            mutableStateOf<String?>(null)
        }

        var chosedStateForEditing by remember {
            mutableStateOf<State?>(null)
        }

        LaunchedEffect(Unit) {
            onBottomStateClicked = { state ->
                chosedStateForEditing = state
                addStateWindowFocused = true
            }

            onBottomTransitionClicked = { transition ->
                choosedTransitionName = transition.name
                if (machineType == MachineType.Pushdown) {
                    transition as PushDownTransition
                    push = transition.push
                    pop = transition.pop
                }
                choosedStateForTransitionStart = getStateByIndex(transition.startState)
                choosedStateForTransitionEnd = getStateByIndex(transition.endState)
                addTransitionWindowFocused = true
            }
        }


        key(currentEditingState) {
            val dragModifier = when (currentEditingState) {

                EditMachineStates.ADD_STATES -> {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val localOffset = getDpOffsetWithPxOffset(
                                Offset(offset.x, offset.y)
                            )
                            clickOffset = localOffset
                            addStateWindowFocused = true
                        }
                    }
                }

                else -> Modifier
            }

            key(recomposition) {
                Transitions(
                    dragModifier = dragModifier,
                    offsetY,
                    offsetX,
                    onTransitionClick = when (currentEditingState) {
                        EditMachineStates.EDITING -> { transition ->
                            choosedStateForTransitionStart = getStateByIndex(transition.startState)
                            choosedStateForTransitionEnd = getStateByIndex(transition.endState)
                            choosedTransitionName = transition.name
                            if (machineType == MachineType.Pushdown) {
                                transition as PushDownTransition
                                push = transition.push
                                pop = transition.pop
                            }
                            addTransitionWindowFocused = true
                        }

                        EditMachineStates.DELETE -> { transition ->
                            deleteTransition(transition)
                            recomposition++
                            increaseRecomposeValue()
                        }

                        else -> null
                    }
                )
                States(dragModifier = dragModifier,
                    currentEditingState,
                    offsetY,
                    offsetX,
                    onStateClick = { state ->
                        when (currentEditingState) {
                            EditMachineStates.ADD_TRANSITIONS -> {
                                choosedStateForTransitionStart = state
                                choosedStateForTransitionEnd = state
                                addTransitionWindowFocused = true
                            }

                            EditMachineStates.DELETE -> {
                                deleteState(state)
                                recomposition++
                            }

                            EditMachineStates.EDITING -> {
                                chosedStateForEditing = state
                                addStateWindowFocused = true
                            }

                            else -> {}
                        }

                    },
                    recompose = {
                        recomposition++
                        increaseRecomposeValue()
                    }
                )
            }

            ToolsRow {
                currentEditingState = editMode
            }
            if (addStateWindowFocused) AddStateWindow(clickOffset, chosedStateForEditing) {
                addStateWindowFocused = false
                chosedStateForEditing = null
                increaseRecomposeValue()
            }
            if (addTransitionWindowFocused) CreateTransitionWindow(
                choosedStateForTransitionStart!!,
                choosedStateForTransitionEnd!!,
                choosedTransitionName,
                push,
                pop
            ) {
                addTransitionWindowFocused = false
                choosedStateForTransitionStart = null
                choosedStateForTransitionEnd = null
                choosedTransitionName = null
                push = null
                pop = null
                increaseRecomposeValue()
            }
        }
    }


    @Composable
    fun EditingMachineBottom(recompose: MutableIntState) {
        key(recompose.intValue){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(MaterialTheme.shapes.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.size(8.dp))
                Text("States", fontSize = 30.sp)
                Spacer(modifier = Modifier.size(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(190.dp)
                        .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .clip(MaterialTheme.shapes.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(states) { state ->

                        Spacer(modifier = Modifier.size(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.background)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    onBottomStateClicked(state)
                                },
                            verticalAlignment = CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(text = "(${state.index}) ${state.name}:${if(state.finite) " final" else ""}${if(state.initial) " initial" else ""}", fontSize = 24.sp)
                        }

                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text("Transitions", fontSize = 30.sp)
                Spacer(modifier = Modifier.size(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(190.dp)
                        .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .clip(MaterialTheme.shapes.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(transitions) { trans ->
                        Spacer(modifier = Modifier.size(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.background)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    onBottomTransitionClicked(trans)
                                },
                            verticalAlignment = CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = "${getStateByIndex(trans.startState).name} -> ${
                                    getStateByIndex(
                                        trans.endState
                                    ).name
                                }: \"${trans.name}\" ${if(machineType==MachineType.Pushdown) "; "+(trans as PushDownTransition).pop+"; "+ trans.push+"." else "."}", fontSize = 24.sp
                            )
                        }

                    }
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getDpOffsetWithPxOffset(pxPosition: Offset): Offset {
        return Offset(
            String.format(
                "%.2f",
                (pxPosition.x) / density.density
            ).replace(',', '.').toFloat(),
            String.format(
                "%.2f",
                (pxPosition.y) / density.density
            ).replace(',', '.').toFloat()
        )
    }

    /**
     * Screen for editing input bar content
     *
     * @param finishedEditing it's a lambda - that invokes when user confirm his changes
     */
    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun EditingInput(finishedEditing: () -> Unit) {
        val inputValue = mutableStateOf(input.toString())
        var editingRecompose by remember { mutableIntStateOf(0) }

        key(editingRecompose) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.size(24.dp))
                Row(modifier = Modifier.fillMaxWidth(0.9f)) {
                    Text(
                        text = stringResource(R.string.editing_input_headline),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    FASButton(text = "ADD") {
                        savedInputs.add(input)
                        input = java.lang.StringBuilder(savedInputs.last().toString()+"")
                        editingRecompose++
                    }
                }

                if (machineType == MachineType.Pushdown) {
                    this@Machine as PushDownMachine
                    val listOfCriteria = listOf(
                        AcceptanceCriteria.BY_FINITE_STATE.text,
                        AcceptanceCriteria.BY_INITIAL_STACK.text
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(fontSize = 24.sp, text = "Accept input by reaching:")
                    Spacer(modifier = Modifier.size(4.dp))
                    DropDownSelector(
                        items = listOfCriteria,
                        defaultSelectedIndex = listOfCriteria.indexOf(acceptanceCriteria.text)
                    ) { newCriteria ->
                        acceptanceCriteria =
                            if (newCriteria.toString() == AcceptanceCriteria.BY_FINITE_STATE.text) AcceptanceCriteria.BY_FINITE_STATE else AcceptanceCriteria.BY_INITIAL_STACK
                    }

                }


                Spacer(modifier = Modifier.size(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.8f)
                        .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FASDefaultTextField(
                        hint = "",
                        value = inputValue.value,
                        requirementText = stringResource(R.string.requirement_text_for_machine_input),
                        onTextChange = { newInput ->
                            input.clear()
                            input.append(newInput)
                            inputValue.value = newInput
                            imuInput = StringBuilder(input.toString())
                        }) {
                        input.contains("^[A-Za-z]+$".toRegex())
                    }
                    savedInputs.forEach { input ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = Arrangement.Center,
                            CenterVertically
                        ) {
                            FASImmutableTextField(text = input.toString(),
                                modifier = Modifier
                                    .clickable {
                                        this@Machine.input = StringBuilder(input.toString())
                                        imuInput = StringBuilder(input.toString())
                                        setInitialStateAsCurrent()
                                        editingRecompose++
                                    }
                                    .width(200.dp)
                                    .height(40.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(painter = painterResource(id = R.drawable.bin),
                                contentDescription = "",
                                modifier = Modifier
                                    .clickable {
                                        savedInputs.remove(input)
                                        editingRecompose++
                                    }
                                    .size(30.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(
                                    id =
                                    if(machineType == MachineType.Finite || ((this@Machine as PushDownMachine).acceptanceCriteria == AcceptanceCriteria.BY_FINITE_STATE)){
                                        if (canReachFinalState(input, true)) {
                                            com.droidhen.theme.R.drawable.check
                                        } else {
                                            com.droidhen.theme.R.drawable.cross
                                        }
                                    } else {
                                        if (canReachInitialStackPDA(input, true, listOf('Z'))) {
                                            com.droidhen.theme.R.drawable.check
                                        } else {
                                            com.droidhen.theme.R.drawable.cross
                                        }
                                    }

                                ),
                                contentDescription = "",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                FASButton(text = "Confirm", onClick = finishedEditing)
            }
        }


    }

    protected fun setInitialStateAsCurrent() {
        currentState?.let {
            getStateByIndex(it).isCurrent = false
        }
        val initialState = states.filter { it.initial }
        if (initialState.isNotEmpty()) {
            initialState[0].isCurrent = true
            currentState = initialState[0].index
            currentTreePosition = 1
        }
        if (machineType == MachineType.Pushdown) {
            (this as PushDownMachine).symbolStack.clear()
            this.symbolStack.add('Z')
        }
    }

    /**
     * private compose function States
     *
     * composes all states of machine on the screen
     * @param dragModifier needed for correct drag and drop of states
     *
     */
    @Composable
    private fun States(
        @SuppressLint("ModifierParameter") dragModifier: Modifier,
        currentEditingState: EditMachineStates? = null,
        offsetY: Float,
        offsetX: Float,
        borderColor: Color = MaterialTheme.colorScheme.tertiary,
        onStateClick: (State) -> Unit,
        recompose: () -> Unit = {}
    ) {

        val currentCircleColor = MaterialTheme.colorScheme.primaryContainer
        states.forEach { state ->
            Box(
                modifier = Modifier
                    .size(state.radius.dp)
                    .offset(state.position.x.dp, state.position.y.dp)
            ) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .then(
                        if (currentEditingState == EditMachineStates.ADD_STATES) dragModifier else dragModifier.pointerInput(
                            Unit
                        ) {
                            if (currentEditingState == EditMachineStates.MOVE) {
                                detectDragGestures(onDrag = { change, dragAmount ->
                                    change.consume()
                                    val localOffset = getDpOffsetWithPxOffset(
                                        Offset(
                                            dragAmount.x,
                                            dragAmount.y
                                        )
                                    )
                                    state.setX(state.position.x + localOffset.x)
                                    state.setY(state.position.y + localOffset.y)


                                }, onDragEnd = { recompose() })
                            } else {
                                detectTapGestures {
                                    onStateClick(state)
                                }
                            }
                        })
                ) {
                    drawCircle(
                        color = borderColor,
                        radius = state.radius + 1,
                        style = Stroke(width = 10f)
                    )
                    drawCircle(
                        color = if (state.isCurrent) currentCircleColor else Color.White,
                        radius = if (state.isCurrent) state.radius - 1 else state.radius
                    )
                    if (state.finite) {
                        drawCircle(
                            color = borderColor,
                            radius = state.radius - 10,
                            style = Stroke(width = 5f)
                        )
                        drawCircle(
                            color = if (state.isCurrent) currentCircleColor else Color.White,
                            radius = if (state.isCurrent) state.radius - 12 else state.radius - 11
                        )
                    }
                    if (state.initial) {
                        val OFFSET = 30f
                        val scaleFactor = 3f

                        val arrowPath = Path().apply {
                            moveTo(size.width * 0.1f - OFFSET * 2, size.height / 2)
                            lineTo(size.width * 0.4f - OFFSET, size.height / 2)
                            lineTo(
                                size.width * 0.35f - OFFSET * 2,
                                size.height / 2 - size.height * 0.05f * scaleFactor
                            )
                            moveTo(size.width * 0.4f - OFFSET, size.height / 2)
                            lineTo(
                                size.width * 0.35f - OFFSET * 2,
                                size.height / 2 + size.height * 0.05f * scaleFactor
                            )
                        }

                        drawPath(
                            path = arrowPath,
                            color = Color.Black,
                            style = Stroke(width = 5f)
                        )
                    }
                }
                Text(
                    text = state.name,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(dragModifier)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
                    style = TextStyle(color = MaterialTheme.colorScheme.tertiary),
                    fontSize = 20.sp
                )
            }
        }
    }


    /**
     * private compose function Transitions
     *
     * composes all transitions of machine
     * @param dragModifier needed for correct drag and drop of transitions
     */
    @Composable
    private fun Transitions(
        @SuppressLint("ModifierParameter") dragModifier: Modifier,
        offsetY: Float,
        offsetX: Float,
        borderColor: Color = MaterialTheme.colorScheme.tertiary,
        onTransitionClick: ((Transition) -> Unit)?
    ) {
        val transitionLocalList = mutableListOf<Transition>()
        val paths = getAllPath { transition -> transitionLocalList.add(transition) }

        val grouped = transitionLocalList.withIndex().groupBy { (_, transition) ->
            Pair(transition.startState, transition.endState)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onTransitionClick == null) dragModifier else dragModifier.pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            for (transition in transitions) {
                                if (transition.isClicked(
                                        tapOffset.x.toDp(),
                                        tapOffset.y.toDp(),
                                        ::getStateByIndex
                                    )
                                ) {
                                    onTransitionClick(transition)
                                    break
                                }
                            }
                        }
                    }
                )
                .onGloballyPositioned { globalCanvasPosition = it }
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            grouped.forEach { (_, indexedTransitions) ->
                val firstIndex = indexedTransitions.first().index
                val path = paths[firstIndex]
                val transition = transitionLocalList[firstIndex]
                val controlPoint = transition.controlPoint!!

                // Рисуем одну стрелку
                drawArrow(path!!, borderColor)

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 58f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                val lineHeight = 52f // увеличен
                val padding = 8f
                val verticalOffset = indexedTransitions.size * 20f
                val baseY =
                    controlPoint.y - ((indexedTransitions.size - 1) * lineHeight / 2) - verticalOffset

                indexedTransitions.forEachIndexed { i, (index, transition) ->
                    val label = buildString {
                        append(transition.name)
                        if (machineType == MachineType.Pushdown && transition is PushDownTransition) {
                            append(";${transition.pop};${transition.push}")
                        }
                    }

                    val y = baseY + i * lineHeight

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        controlPoint.x,
                        y + padding,
                        paint
                    )
                }
            }

        }
    }


    abstract fun addNewState(state: State)

    /**
     * checks if machine already has state with the same name
     * if so - modifies already existing state (extends name of existing state)
     * behaviour of this function can be changed in children of Machine class
     *
     * @param name - name of new state
     * @param startState and endState - states of transition
     */
    private fun addNewTransition(name: String, startState: State, endState: State) {
        var iterations = 0
        transitions.filter { transition ->
            transition.startState == startState.index && transition.endState == endState.index && transition.name == name
        }.forEach {
            iterations++
            it.name += name
        }
        if (iterations == 0) {
            transitions.add(Transition(name, startState.index, endState.index))
        }
    }

    private fun addNewTransition(
        name: String,
        startState: State,
        endState: State,
        pop: String,
        checkState: String,
        push: String
    ) {

        transitions.add(
            if (pop != "") {
                if (transitions.any {
                        it as PushDownTransition
                        it.name == name && it.startState == startState.index && it.endState == endState.index && it.pop == pop && it.push == ""
                    }) return
                PushDownTransition(
                    name,
                    startState.index,
                    endState.index,
                    pop = pop,
                    push = "",
                )
            } else if (checkState != "") {
                if (transitions.any {
                        it as PushDownTransition
                        it.name == name && it.startState == startState.index && it.endState == endState.index && it.pop == checkState && it.push == push + checkState
                    }) return
                PushDownTransition(
                    name,
                    startState.index,
                    endState.index,
                    checkState,
                    push + checkState
                )
            } else throw Exception("Wrong transition type")
        )
    }


    /**
     * InputBar
     *
     * Compose function that creates bar that shows input chars for the machine
     */
    @Composable
    private fun InputBar() {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(light_blue)
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = CenterVertically
            ) {
                itemsIndexed(input.toString().toCharArray().toList()) { index, inputChar ->
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (index == 0) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            index.toString(),
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            inputChar.toString(),
                            fontSize = 24.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }
    }


    /**
     * Animates a value based on the current state of a transition.
     *
     * This function creates a [State] object that animates a value between 0f and 1f
     * as the provided [transition] progresses between states. The animation is driven
     * by the [targetState] and the current state of the transition.
     *
     * @param targetState The target state for the animation. The animation will
     * progress towards 1f when the transition is in this state.
     * @param transition The transition that drives the animation.
     * @param label An optional label for the animation, used for debugging purposes.
     *
     * @return A [State] object that represents the animated value.
     */
    @Composable
    protected fun AnimationOfTransition(
        start: Offset,
        end: Offset,
        radius: Float,
        duration: Int = 500,
        onAnimationEnd: () -> Unit,
    ) {
        val progress = remember { Animatable(0f) }
        val isCanvasVisible = remember {
            mutableStateOf(true)
        }
        LaunchedEffect(Unit) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(duration, easing = FastOutSlowInEasing)
            )
            isCanvasVisible.value = false
            onAnimationEnd()
        }

        if (isCanvasVisible.value) {
            val circleColor = MaterialTheme.colorScheme.primary
            val radiusBigCircle = radius / 2
            val radiusSmallCircle = radiusBigCircle - 5
            val path = getTransitionByPath(startState = start, endState = end) {}

            Canvas(modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXGraph.roundToInt(), offsetYGraph.roundToInt()) }) {

                val currentPosition = getCurrentPositionByPath(
                    path.first!!,
                    progress.value * if (start == end) 0.8f else 1f
                )
                drawCircle(color = circleColor, radius = radiusBigCircle, center = currentPosition)
                drawCircle(
                    color = Color.White,
                    radius = radiusSmallCircle,
                    center = currentPosition
                )
            }
        }
    }

    /**
     * return position of transition point by path and progress of point had made
     * @param path - path of point
     * @param progress - progress of point had made
     *
     * @return Offset - position of point
     */
    private fun getCurrentPositionByPath(path: Path, progress: Float): Offset {
        val currentPositionArray = FloatArray(2)
        val pathMeasure = PathMeasure(path.asAndroidPath(), false)
        pathMeasure.getPosTan(pathMeasure.length * progress, currentPositionArray, null)
        return Offset(currentPositionArray[0], currentPositionArray[1])
    }

    @Composable
    private fun ToolsRow(changedMode: (EditMachineStates) -> Unit) {
        val spaceSize = 20.dp
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.13f)
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(perlamutr_white)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(13f)
                    .background(perlamutr_white),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.edit_icon),
                    contentDescription = stringResource(R.string.edit_icon),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(if (editMode == EditMachineStates.EDITING) MaterialTheme.colorScheme.primaryContainer else Color.White)
                        .clickable {
                            editMode = EditMachineStates.EDITING
                            changedMode(editMode)
                        }
                )
                Spacer(modifier = Modifier.width(spaceSize))
                Icon(
                    painter = painterResource(id = R.drawable.add_states),
                    contentDescription = stringResource(
                        R.string.add_states_icon
                    ),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(if (editMode == EditMachineStates.ADD_STATES) MaterialTheme.colorScheme.primaryContainer else Color.White)
                        .clickable {
                            editMode = EditMachineStates.ADD_STATES
                            changedMode(editMode)
                        }
                )
                Spacer(modifier = Modifier.width(spaceSize))
                Icon(
                    painter = painterResource(id = R.drawable.add_transitions),
                    contentDescription = stringResource(
                        R.string.add_transitions_icon
                    ),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(if (editMode == EditMachineStates.ADD_TRANSITIONS) MaterialTheme.colorScheme.primaryContainer else Color.White)
                        .clickable {
                            editMode = EditMachineStates.ADD_TRANSITIONS
                            changedMode(editMode)
                        }
                )
                Spacer(modifier = Modifier.width(spaceSize))
                Icon(
                    painter = painterResource(id = R.drawable.bin),
                    contentDescription = stringResource(
                        R.string.bin_icon
                    ),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(if (editMode == EditMachineStates.DELETE) MaterialTheme.colorScheme.primaryContainer else Color.White)
                        .clickable {
                            editMode = EditMachineStates.DELETE
                            changedMode(editMode)
                        }
                )
                Spacer(modifier = Modifier.width(spaceSize))
                Icon(
                    painter = painterResource(id = R.drawable.move_icon),
                    contentDescription = stringResource(
                        R.string.move_icon
                    ),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(if (editMode == EditMachineStates.MOVE) MaterialTheme.colorScheme.primaryContainer else Color.White)
                        .clickable {
                            editMode = EditMachineStates.MOVE
                            changedMode(editMode)
                        }
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(perlamutr_white)
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }

    /**
     * composes addTransition window, where user able to add new states
     * supposed to have startState
     *
     * @param startToState - start state of transition
     */
    @Composable
    private fun CreateTransitionWindow(
        start: State,
        end: State,
        nameParam: String?,
        push: String?,
        pop: String?,
        onFinished: () -> Unit
    ) {
        var name by remember {
            mutableStateOf(nameParam ?: "")
        }

        var startState: State by remember {
            mutableStateOf(start)
        }
        var endState: State by remember {
            mutableStateOf(end)
        }
        var popVal by remember {
            mutableStateOf(pop ?: "")
        }
        var checkStack by remember {
            mutableStateOf("")
        }
        var pushVal by remember {
            mutableStateOf(push ?: "")
        }
        var pdaTransitionType by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            if (machineType == MachineType.Pushdown && nameParam != null) {
                if (pushVal.isNotEmpty()) {
                    pushVal = pushVal.removeSuffix(pushVal.last().toString())
                    checkStack = popVal + "*"
                    checkStack = checkStack.removeSuffix("*")
                    pdaTransitionType = "push"
                } else if (pushVal.isEmpty()) {
                    pdaTransitionType = "pop"
                }
            }
        }


        DefaultFASDialogWindow(
            title = if (nameParam == null) stringResource(R.string.new_transition) else "editing transition: $name",
            onDismiss = { onFinished() },
            onConfirm = {
                if (nameParam == null) {
                    if (machineType == MachineType.Finite) {
                        addNewTransition(
                            name = name,
                            startState = startState,
                            endState = endState
                        )
                    } else {
                        addNewTransition(
                            name = name,
                            startState = startState,
                            endState = endState,
                            pop = popVal,
                            checkState = checkStack,
                            push = pushVal
                        )
                    }
                } else {
                    if (machineType == MachineType.Finite) {
                        transitions.filter { transition ->
                            transition.startState == start.index && transition.endState == end.index
                        }[0].let {
                            it.name = name
                            it.startState = startState.index
                            it.endState = endState.index
                        }
                    } else {
                        transitions as MutableList<PushDownTransition>
                        transitions.filter { transition ->
                            transition.startState == start.index && transition.endState == end.index && transition.push == (push
                                ?: "") && transition.pop == (pop ?: "")
                        }[0].let {
                            it.name = name
                            it.startState = startState.index
                            it.endState = endState.index
                            if (pdaTransitionType == "pop") {
                                it.pop = popVal
                                it.push = ""
                            } else {
                                it.pop = checkStack
                                it.push = pushVal + checkStack
                            }
                        }
                    }
                }
                onFinished()
            },
            modifier = if (machineType == MachineType.Pushdown) Modifier.height(650.dp) else Modifier
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalAlignment = CenterVertically
            ) {
                FASDefaultTextField(
                    hint = "transition name",
                    value = name,
                    requirementText = "",
                    onTextChange = { name = it }) { true }
            }
            Spacer(modifier = Modifier.height(8.dp))

            /**
             * choosing states for transition
             */
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp), verticalAlignment = CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "from")
                Spacer(modifier = Modifier.width(8.dp))
                DropDownSelector(
                    items = states,
                    label = "start state",
                    defaultSelectedIndex = states.indexOf(start)
                ) { selectedItem ->
                    startState = selectedItem as State
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "to")
                Spacer(modifier = Modifier.width(8.dp))
                DropDownSelector(
                    items = states,
                    label = "end state",
                    defaultSelectedIndex = states.indexOf(end)
                ) { selectedItem ->
                    endState = selectedItem as State
                }
            }

            /**
             *what type of transition (pop/push)
             */
            if (machineType == MachineType.Pushdown) {
                DropDownSelector(
                    items = listOf("pop", "push"),
                    defaultSelectedIndex = if (pdaTransitionType == "pop") 0 else 1
                ) { selected ->
                    pdaTransitionType = selected.toString()
                }
                Spacer(modifier = Modifier.size(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pdaTransitionType == "pop") {
                        FASDefaultTextField(
                            hint = "pop",
                            value = popVal,
                            requirementText = "length is 1",
                            onTextChange = {
                                popVal = it
                                pushVal = ""
                                checkStack = ""
                            },
                            modifier = Modifier
                                .width(96.dp)
                                .height(70.dp)
                                .padding(start = 16.dp)
                        ) {
                            popVal.length == 1

                        }
                        Icon(
                            painter = painterResource(id = com.droidhen.theme.R.drawable.question),
                            contentDescription = "",
                            modifier = Modifier
                                .fillMaxHeight(0.3f)
                                .width(46.dp)
                                .padding(end = 16.dp)
                                .clickable {
                                    Toast
                                        .makeText(
                                            context,
                                            "set the char that will be popped from the top of the stack",
                                            Toast.LENGTH_LONG
                                        )
                                        .show()
                                })
                    } else if (pdaTransitionType == "push") {
                        FASDefaultTextField(
                            hint = "pop-check",
                            value = checkStack,
                            requirementText = "length is 1",
                            onTextChange = {
                                checkStack = it
                                popVal = ""
                            },
                            modifier = Modifier
                                .width(130.dp)
                                .height(60.dp)
                                .padding(start = 16.dp)
                        ) {
                            checkStack.length == 1
                        }
                        FASDefaultTextField(
                            hint = "push",
                            value = pushVal,
                            requirementText = "max length is 1",
                            onTextChange = {
                                pushVal = it
                                popVal = ""
                            },
                            modifier = Modifier
                                .width(80.dp)
                                .height(60.dp)

                        ) {
                            pushVal.length <= 1
                        }
                        Icon(
                            painter = painterResource(id = com.droidhen.theme.R.drawable.question),
                            contentDescription = "",
                            modifier = Modifier
                                .fillMaxHeight(0.25f)
                                .width(42.dp)
                                .padding(end = 16.dp)
                                .clickable {
                                    Toast
                                        .makeText(
                                            context,
                                            "set the char that will be checked on the top of the stack",
                                            Toast.LENGTH_LONG
                                        )
                                        .show()
                                    Toast
                                        .makeText(
                                            context,
                                            "set the char that will be pushed to the stack",
                                            Toast.LENGTH_LONG
                                        )
                                        .show()
                                })
                    }
                }
            }
        }
    }

    private fun findNewStateIndex(): Int {
        val sortedStates = states.sortedBy { it.index }
        return if (states.isEmpty()) 1 else if (sortedStates.last().index == sortedStates.size) sortedStates.last().index + 1 else {
            var previousItem = 0
            var choosedIndex = sortedStates.size + 1
            sortedStates.forEach { item ->
                if (item.index == previousItem + 1) {
                    previousItem = item.index
                } else {
                    choosedIndex = previousItem + 1
                }
            }
            return choosedIndex
        }
    }

    /**
     * composes AddStateWindow
     *
     * Shows window to create/edit states
     * @param clickOffset - provides coordinates of click, where should be created new state
     * @param finished - lambda that invokes when user confirm his changes
     */
    @Composable
    private fun AddStateWindow(clickOffset: Offset, chosedState: State?, finished: () -> Unit) {

        var name by remember {
            mutableStateOf(chosedState?.name ?: "")
        }
        var initial by remember {
            mutableStateOf(chosedState?.initial ?: false)
        }
        var finite by remember {
            mutableStateOf(chosedState?.finite ?: false)
        }

        DefaultFASDialogWindow(
            title = if (chosedState == null) stringResource(id = R.string.new_state) else "edit state: ${chosedState.name}",
            conditionToEnable = name.isNotEmpty(),
            onDismiss = {
                finished()
            },
            onConfirm = {
                if (chosedState == null) {
                    addNewState(
                        State(
                            name = name,
                            isCurrent = false,
                            index = findNewStateIndex(),
                            finite = finite,
                            initial = initial,
                            position = clickOffset
                        )
                    )
                } else {
                    chosedState.name = name
                    chosedState.initial = initial
                    chosedState.finite = finite
                }
                finished()
            }) {

            /**
             * Row for initial and finite state
             */
            /**
             * Row for initial and finite state
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ItemSpecificationIcon(
                    icon = R.drawable.initial_state_icon,
                    text = "initial",
                    isActive = initial
                ) {
                    if (chosedState != null || checkMachineForExistingInitialState()) initial =
                        !initial
                }

                ItemSpecificationIcon(
                    icon = R.drawable.finite_state_icon,
                    text = "finite",
                    isActive = finite
                ) {
                    if (chosedState != null || checkMachineForExistingFiniteState()) finite =
                        !finite
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalAlignment = CenterVertically
            ) {
                Text(text = "x: ${clickOffset.x}", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "y: ${clickOffset.y}", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalAlignment = CenterVertically
            ) {
                FASDefaultTextField(
                    hint = "name",
                    value = name,
                    requirementText = "",
                    onTextChange = { name = it }) { true }
            }
        }
    }

    private fun checkMachineForExistingFiniteState(): Boolean {
        return if (states.any { it.finite }) {
            Toast.makeText(context, "Your machine already has finite state", Toast.LENGTH_SHORT)
                .show()
            false
        } else true
    }

    private fun checkMachineForExistingInitialState(): Boolean {
        return if (states.any { it.initial }) {
            Toast.makeText(
                context,
                "Your machine already has initial state",
                Toast.LENGTH_SHORT
            )
                .show()
            false
        } else true
    }


    abstract fun getDerivationTreeElements(): List<List<TreeNode>>

    @Composable
    fun DerivationTree() {
        val derivationTreeElements = remember {
            mutableStateOf<List<List<TreeNode>>>(listOf())
        }

        LaunchedEffect(Unit) {
            derivationTreeElements.value = getDerivationTreeElements()
        }

        if (derivationTreeElements.value.isNotEmpty()) {
            val sumOfLeafesWeight = derivationTreeElements.value.last().sumOf { node ->
                node.weight.toInt()
            }
            val height =
                if (sumOfLeafesWeight <= 10) 350.dp else 350.dp + ((sumOfLeafesWeight - 10) * 30).dp
            LazyColumn(
                Modifier
                    .height(350.dp)
                    .fillMaxWidth()
                    .background(perlamutr_white)
                    .clip(MaterialTheme.shapes.large)
                    .border(3.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
            ) {
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height),
                    ) {
                        items(derivationTreeElements.value) { treeLevel ->
                            Column(
                                modifier = Modifier.width(30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                treeLevel.forEach { treeNode ->
                                    if (treeNode.stateName != null) {
                                        val backgroundColor = if (treeNode.isAccepted) {
                                            if (treeNode.isCurrent) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.background
                                            }
                                        } else {
                                            if (treeNode.isCurrent) unable_views else MaterialTheme.colorScheme.errorContainer
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.medium)
                                                .weight(treeNode.weight)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.shapes.medium
                                                )
                                                .background(backgroundColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = treeNode.stateName, fontSize = 18.sp)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(treeNode.weight))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                    }
                }
            }
        }

    }

    @Composable
    abstract fun MathFormat()

    abstract fun canReachFinalState(input: StringBuilder, fromInit:Boolean): Boolean

    abstract fun exportToJFF(): String
}

sealed class MachineType(val tag: String) {
    object Finite : MachineType("fa")
    object Pushdown : MachineType("pda")

    override fun toString(): String = tag
}

data class TreeNode(
    val stateName: String?, // null если мёртвый узел
    val weight: Float = 1f,
    val isCurrent: Boolean = false,
    val isAccepted: Boolean = false
)
