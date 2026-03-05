package com.sfag.automata.ui.editor

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.launch

import com.sfag.R
import com.sfag.automata.EDGE_LINE_WIDTH
import com.sfag.automata.EDGE_TEXT_SIZE
import com.sfag.automata.NODE_RADIUS
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.MachineType
import com.sfag.automata.model.machine.State
import com.sfag.automata.model.transition.PushdownTransition
import com.sfag.automata.model.transition.Transition
import com.sfag.automata.ui.diagram.TransitionRenderer
import com.sfag.automata.ui.diagram.computePaths
import com.sfag.shared.Symbols
import com.sfag.shared.ui.component.DefaultDialogWindow
import com.sfag.shared.ui.component.DefaultTextField
import com.sfag.shared.ui.component.DropDownSelector

/** Renders all transitions as tappable bezier arrows. */
@Composable
internal fun Machine.Transitions(
    positions: Map<Int, Offset>,
    modifier: Modifier,
    offsetY: Float,
    offsetX: Float,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
    onTransitionClick: ((Transition) -> Unit)?
) {
    val density = LocalDensity.current
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val renderDataList = computePaths(positions, density)

    // Group by (startState, endState) to stack labels for same-pair transitions on one arrow
    val grouped = transitions.withIndex().groupBy { (_, transition) ->
        Pair(transition.startState, transition.endState)
    }

    data class ArrowDrawData(
        val body: Path,
        val head: Path,
        val controlPoint: Offset,
        val textPosition: Offset,
        val indexedTransitions: List<IndexedValue<Transition>>,
        val isSelfLoop: Boolean
    )

    val drawDataList = grouped.mapNotNull { (_, indexedTransitions) ->
        val firstIndex = indexedTransitions.first().index
        val renderData = renderDataList.getOrNull(firstIndex) ?: return@mapNotNull null
        ArrowDrawData(renderData.bodyPath, renderData.headPath, renderData.controlPoint, renderData.textPosition, indexedTransitions, renderData.isSelfLoop)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (onTransitionClick == null) modifier else modifier.pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val adjustedX = tapOffset.x - offsetX
                        val adjustedY = tapOffset.y - offsetY
                        for ((idx, renderData) in renderDataList.withIndex()) {
                            if (renderData != null && isTransitionHit(renderData, adjustedX, adjustedY)) {
                                onTransitionClick(transitions[idx])
                                break
                            }
                        }
                    }
                }
            )
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
    ) {
        // Pass 1: Draw all arrow bodies
        for (data in drawDataList) {
            drawPath(
                data.body,
                color = borderColor,
                style = Stroke(width = EDGE_LINE_WIDTH, cap = StrokeCap.Round)
            )
        }

        // Pass 2: Draw all arrow heads (filled triangle)
        for (data in drawDataList) {
            drawPath(data.head, color = borderColor)
        }

        // Pass 3: Draw all labels
        val scaledTextSize = EDGE_TEXT_SIZE * density.density
        val paint = Paint().apply {
            color = colorOnSurface.toArgb()
            textSize = scaledTextSize
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val lineHeight = scaledTextSize * 1.25f
        val selfLoopClearance = scaledTextSize * 0.25f + EDGE_LINE_WIDTH
        val normalClearance = scaledTextSize * 0.5f + EDGE_LINE_WIDTH

        for (data in drawDataList) {
            val labelClearance = if (data.isSelfLoop) selfLoopClearance else normalClearance
            val count = data.indexedTransitions.size
            val labels = data.indexedTransitions.map { (_, transition) ->
                buildString {
                    append(transition.name.ifEmpty { "${Symbols.EPSILON}" })
                    if (machineType == MachineType.Pushdown && transition is PushdownTransition) {
                        val pop = transition.pop.ifEmpty { "${Symbols.EPSILON}" }
                        val push = transition.push.ifEmpty { "${Symbols.EPSILON}" }
                        append(", $pop;$push")
                    }
                }
            }

            val maxTextWidth = labels.maxOf { paint.measureText(it) }
            val halfW = (maxTextWidth * 0.5f).coerceAtLeast(1f)
            val halfH = (count * scaledTextSize * 0.625f).coerceAtLeast(1f)

            val dpx = data.textPosition.x - data.controlPoint.x
            val dpy = data.textPosition.y - data.controlPoint.y
            val dLen = sqrt(dpx * dpx + dpy * dpy)
            val nx = if (dLen > 0.1f) dpx / dLen else 0f
            val ny = if (dLen > 0.1f) dpy / dLen else 1f

            val nxW = nx / halfW
            val nyH = ny / halfH
            val denomSq = nxW * nxW + nyH * nyH
            val edgeDist = if (denomSq > 0f) 1f / sqrt(denomSq) else halfH

            val adjustment = edgeDist + labelClearance - NODE_RADIUS * 2f
            val centerX = data.textPosition.x + nx * adjustment
            val centerY = data.textPosition.y + ny * adjustment

            val baseY = centerY - (count - 1) * lineHeight * 0.5f + scaledTextSize * 0.375f
            labels.forEachIndexed { i, label ->
                drawContext.canvas.nativeCanvas.drawText(label, centerX, baseY + i * lineHeight, paint)
            }
        }
    }
}

private fun isTransitionHit(renderData: TransitionRenderer, tapX: Float, tapY: Float, threshold: Float = 40f): Boolean {
    val cp = renderData.controlPoint
    val dx = tapX - cp.x
    val dy = tapY - cp.y
    return (dx * dx + dy * dy) < threshold * threshold
}

/** Dialog for creating or editing a transition. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Machine.CreateTransitionWindow(
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
                pushVal = pushVal.dropLast(1)
                checkStack = popVal
                pdaTransitionType = "push"
            } else {
                pdaTransitionType = "pop"
            }
        }
    }

    DefaultDialogWindow(
        title = null,
        onDismiss = onFinished,
        onConfirm = {
            if (nameParam == null) {
                if (machineType == MachineType.Finite) {
                    addNewTransition(
                        name = name,
                        startState = startState,
                        endState = endState
                    )
                } else {
                    (this as? com.sfag.automata.model.machine.PushdownMachine)?.addNewTransition(
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
                    transitions.firstOrNull { transition ->
                        transition.startState == start.index && transition.endState == end.index
                    }?.let {
                        it.name = name
                    }
                } else {
                    transitions.filterIsInstance<PushdownTransition>().firstOrNull { transition ->
                        transition.startState == start.index && transition.endState == end.index &&
                            transition.push == (push ?: "") && transition.pop == (pop ?: "")
                    }?.let {
                        it.name = name
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
    ) {
        // Stack operation section (PDA only) - on top
        if (machineType == MachineType.Pushdown) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.stack_operation),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val tooltipState = rememberTooltipState()
                val scope = rememberCoroutineScope()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = {
                        PlainTooltip {
                            Column {
                                Text(stringResource(R.string.help_pop))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.help_push))
                            }
                        }
                    },
                    state = tooltipState
                ) {
                    IconButton(
                        onClick = { scope.launch { tooltipState.show() } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.help),
                            contentDescription = stringResource(R.string.stack_operation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val popLabel = stringResource(R.string.pop)
            val pushLabel = stringResource(R.string.push)
            DropDownSelector(
                items = listOf(popLabel, pushLabel),
                defaultSelectedIndex = if (pdaTransitionType == "pop") 0 else 1,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge
            ) { selected ->
                pdaTransitionType = if (selected.toString() == popLabel) "pop" else "push"
                if (pdaTransitionType == "pop") {
                    checkStack = ""
                    pushVal = ""
                } else {
                    popVal = ""
                }
            }

            // Fixed height box to prevent dialog bouncing when switching modes
            Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                if (pdaTransitionType == "pop") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DefaultTextField(
                            hint = stringResource(R.string.pop),
                            value = popVal,
                            onTextChange = {
                                popVal = it
                                pushVal = ""
                                checkStack = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            popVal.length == 1
                        }
                    }
                } else if (pdaTransitionType == "push") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DefaultTextField(
                            hint = stringResource(R.string.pop_check),
                            value = checkStack,
                            onTextChange = {
                                checkStack = it
                                popVal = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            checkStack.length == 1
                        }
                        DefaultTextField(
                            hint = stringResource(R.string.push),
                            value = pushVal,
                            onTextChange = {
                                pushVal = it
                                popVal = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            pushVal.length <= 1
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Transition name field
        DefaultTextField(
            hint = stringResource(R.string.transition_name),
            value = name,
            modifier = Modifier.fillMaxWidth(),
            onTextChange = { name = it })

        // From/To dropdowns row
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(R.string.from_label), style = MaterialTheme.typography.bodyMedium)
            DropDownSelector(
                items = states,
                label = stringResource(R.string.start_state),
                defaultSelectedIndex = states.indexOf(start),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge
            ) { val s = it as State; if (s != startState) startState = s }
            Text(text = stringResource(R.string.to_label), style = MaterialTheme.typography.bodyMedium)
            DropDownSelector(
                items = states,
                label = stringResource(R.string.end_state),
                defaultSelectedIndex = states.indexOf(end),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge
            ) { val s = it as State; if (s != endState) endState = s }
        }
    }
}
