package com.sfag.automata.presentation.component.edit

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.presentation.extension.drawArrow
import com.sfag.R
import com.sfag.automata.presentation.component.widget.DefaultDialogWindow
import com.sfag.automata.presentation.component.widget.DefaultTextField
import com.sfag.automata.presentation.component.widget.DropDownSelector
import kotlin.math.roundToInt

/**
 * private compose function Transitions
 *
 * composes all transitions of machine
 * @param dragModifier needed for correct drag and drop of transitions
 */
@Composable
internal fun Machine.Transitions(
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
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
    ) {
        grouped.forEach { (_, indexedTransitions) ->
            val firstIndex = indexedTransitions.first().index
            val path = paths[firstIndex] ?: return@forEach
            val transition = transitionLocalList[firstIndex]
            val controlPoint = transition.controlPoint ?: return@forEach

            // Draw one arrow
            drawArrow(path, borderColor)

            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 58f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            val lineHeight = 52f
            val padding = 8f
            val verticalOffset = indexedTransitions.size * 20f
            val baseY =
                controlPoint.y - ((indexedTransitions.size - 1) * lineHeight / 2) - verticalOffset

            indexedTransitions.forEachIndexed { i, (_, transition) ->
                val label = buildString {
                    append(transition.name)
                    if (machineType == MachineType.Pushdown && transition is PushDownTransition) {
                        append(", ${transition.pop};${transition.push}")
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


/**
 * composes addTransition window, where user able to add new states
 * supposed to have startState
 *
 * @param startToState - start state of transition
 */
@Composable
internal fun Machine.CreateTransitionWindow(
    start: State,
    end: State,
    nameParam: String?,
    push: String?,
    pop: String?,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
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
                checkStack = "$popVal*"
                checkStack = checkStack.removeSuffix("*")
                pdaTransitionType = "push"
            } else if (pushVal.isEmpty()) {
                pdaTransitionType = "pop"
            }
        }
    }


    DefaultDialogWindow(
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
                    transitions.filterIsInstance<PushDownTransition>().filter { transition ->
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
            DefaultTextField(
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
                    DefaultTextField(
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
                        painter = painterResource(id = R.drawable.question),
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
                    DefaultTextField(
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
                    DefaultTextField(
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
                        painter = painterResource(id = R.drawable.question),
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
