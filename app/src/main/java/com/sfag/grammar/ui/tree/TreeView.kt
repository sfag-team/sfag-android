package com.sfag.grammar.ui.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withSave
import com.sfag.grammar.domain.grammar.GrammarType
import com.sfag.grammar.domain.tree.TreeNode
import com.sfag.main.config.MAX_ZOOM
import com.sfag.main.config.MIN_ZOOM
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val TREE_NODE_TEXT_SIZE = 20.sp
private const val TREE_NODE_RADIUS = 40f
private const val DASH_INTERVAL = 10f
private const val GROUPING_RECT_PADDING = 15f
private const val HIGHLIGHT_STROKE_WIDTH = 15f

@Composable
fun TreeView(
    root: TreeNode,
    positions: Map<TreeNode, NodeLayout>,
    scale: MutableState<Float>,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    currentStep: Int,
    grammarType: GrammarType,
    canvasSize: MutableState<Size>
) {
    fun pos(node: TreeNode) = positions[node]!!

    val allNodes =
        remember(root, currentStep, grammarType) {
            val collected = mutableSetOf<TreeNode>()
            root.collect(collected, currentStep)
            val filtered = collected.filter { positions[it] != null }.toMutableSet()
            if (grammarType == GrammarType.UNRESTRICTED || grammarType == GrammarType.CONTEXT_SENSITIVE) {
                filtered
                    .sortedWith(compareBy({ pos(it).depth.max() }, { pos(it).x }))
                    .toCollection(LinkedHashSet())
            } else {
                filtered
            }
        }

    // Pre-compute node y-coordinates
    remember(allNodes, currentStep) {
        allNodes.onEach { node ->
            val nodePos = pos(node)
            nodePos.y =
                if (node.children.isNotEmpty() && currentStep < node.children.first().step) {
                    nodePos.depth.min()
                } else {
                    nodePos.depth.max()
                }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val nonTerminalColor = colorScheme.primaryContainer
    val terminalColor = colorScheme.tertiaryContainer
    val highlightColor = colorScheme.tertiary
    val groupingRectColor = colorScheme.outline
    val edgeColor = colorScheme.inverseOnSurface
    val nodeTextColor = colorScheme.onPrimaryContainer
    val leafHighlightColor = colorScheme.secondary
    val textMeasurer = rememberTextMeasurer()

    val maxStep = remember(allNodes) { allNodes.maxOfOrNull { it.step } ?: 0 }
    val leafColor = if (currentStep == maxStep + 1) leafHighlightColor else nodeTextColor

    val textLayouts =
        remember(allNodes, nodeTextColor, leafColor) {
            allNodes.associateWith { node ->
                val textColor = if (node.children.isEmpty()) leafColor else nodeTextColor
                textMeasurer.measure(
                    text = node.label.toString(),
                    style =
                        TextStyle(
                            fontSize = TREE_NODE_TEXT_SIZE,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                )
            }
        }

    val edges =
        remember(allNodes, currentStep) {
            val visited = mutableSetOf<TreeNode>()
            val result = mutableListOf<Edge>()
            allNodes.forEach { node ->
                if (node.children.isNotEmpty() && node.children.first().step <= currentStep) {
                    for (child in node.children) {
                        if (!visited.add(child)) continue
                        val px =
                            if (child.parents.size > 1) {
                                child.parents
                                    .map { pos(it).x }
                                    .average()
                                    .toFloat()
                            } else {
                                pos(node).x
                            }
                        result.add(Edge(px, pos(node).y, child))
                    }
                }
            }
            result
        }

    var treeBounds by remember { mutableStateOf<Rect?>(null) }
    SideEffect {
        treeBounds =
            if (allNodes.isNotEmpty()) {
                Rect(
                    left = allNodes.minOf { pos(it).x },
                    top = allNodes.minOf { pos(it).y },
                    right = allNodes.maxOf { pos(it).x },
                    bottom = allNodes.maxOf { pos(it).y }
                )
            } else {
                null
            }
    }

    val hasHighlight =
        remember(allNodes, currentStep) {
            allNodes.any { node ->
                (node.children.isNotEmpty() && node.children.first().step == currentStep) ||
                    node.step == currentStep
            }
        }
    val infiniteTransition = rememberInfiniteTransition(label = "dash-animation")
    val phaseState =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 20f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
            label = "phase"
        )
    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { layoutSize ->
                    canvasSize.value = Size(layoutSize.width.toFloat(), layoutSize.height.toFloat())
                }.pointerInput(Unit) {
                    coroutineScope {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale.value =
                                (scale.value * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            var newX = offsetX.value + pan.x
                            var newY = offsetY.value + pan.y
                            val bounds = treeBounds
                            val viewportSize = canvasSize.value
                            if (bounds != null && viewportSize.width > 0f && viewportSize.height > 0f) {
                                val nodeR = TREE_NODE_RADIUS
                                val currentScale = scale.value
                                newX =
                                    newX.coerceIn(
                                        -(bounds.right + nodeR) * currentScale,
                                        viewportSize.width - (bounds.left - nodeR) * currentScale
                                    )
                                newY =
                                    newY.coerceIn(
                                        -(bounds.bottom + nodeR) * currentScale,
                                        viewportSize.height - (bounds.top - nodeR) * currentScale
                                    )
                            }
                            launch {
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                        }
                    }
                }.clipToBounds()
    ) {
        val pathEffect =
            if (hasHighlight) {
                PathEffect.dashPathEffect(floatArrayOf(DASH_INTERVAL, DASH_INTERVAL), phaseState.value)
            } else {
                null
            }
        with(drawContext.canvas.nativeCanvas) {
            withSave {
                translate(offsetX.value, offsetY.value)
                scale(scale.value, scale.value)
                drawTree(
                    nodes = allNodes,
                    pos = positions,
                    nodeRadius = TREE_NODE_RADIUS,
                    textLayouts = textLayouts,
                    edges = edges,
                    currentStep = currentStep,
                    highlightEffect = pathEffect,
                    nonTerminalColor = nonTerminalColor,
                    terminalColor = terminalColor,
                    animationHighlightColor = highlightColor,
                    groupingRectColor = groupingRectColor,
                    edgeColor = edgeColor,
                    nodeTextColor = nodeTextColor
                )
            }
        }
    }
}

private data class Edge(
    val parentX: Float,
    val parentY: Float,
    val child: TreeNode
)

private fun DrawScope.drawTree(
    nodes: Collection<TreeNode>,
    pos: Map<TreeNode, NodeLayout>,
    nodeRadius: Float,
    textLayouts: Map<TreeNode, androidx.compose.ui.text.TextLayoutResult>,
    edges: List<Edge>,
    currentStep: Int,
    highlightEffect: PathEffect?,
    nonTerminalColor: Color,
    terminalColor: Color,
    animationHighlightColor: Color,
    groupingRectColor: Color,
    edgeColor: Color,
    nodeTextColor: Color
) {

    for (edge in edges) {
        drawEdge(edge.parentX, edge.parentY, edge.child, pos, edgeColor)
    }

    nodes.forEach { node ->
        val nodePos = pos[node]!!

        if (
            node.children.isNotEmpty() &&
            node.children
                .first()
                .parents.size > 1 &&
            currentStep >= node.children.first().step &&
            node.children
                .first()
                .parents
                .first() == node
        ) {
            drawRoundRect(
                color = groupingRectColor,
                topLeft = Offset(nodePos.x - GROUPING_RECT_PADDING - nodeRadius, nodePos.y - GROUPING_RECT_PADDING - nodeRadius),
                size =
                    Size(
                        pos[node.children
                                .first()
                                .parents
                                .last()]!!
                        .x + nodeRadius + GROUPING_RECT_PADDING -
                            (nodePos.x - GROUPING_RECT_PADDING - nodeRadius),
                        nodePos.y + nodeRadius + GROUPING_RECT_PADDING - (nodePos.y - GROUPING_RECT_PADDING - nodeRadius)
                    ),
                cornerRadius = CornerRadius(nodeRadius, nodeRadius)
            )
        }

        val color =
            if (!node.label.isDigit() && node.label.isUpperCase()) {
                nonTerminalColor
            } else {
                terminalColor
            }

        if (node.children.isNotEmpty() && node.children.first().step == currentStep) {
            drawCircle(
                animationHighlightColor,
                center = Offset(nodePos.x, nodePos.y),
                radius = nodeRadius,
                style = Stroke(width = HIGHLIGHT_STROKE_WIDTH, pathEffect = highlightEffect)
            )
        } else if (node.step == currentStep) {
            drawCircle(
                nodeTextColor,
                center = Offset(nodePos.x, nodePos.y),
                radius = nodeRadius,
                style = Stroke(width = HIGHLIGHT_STROKE_WIDTH, pathEffect = highlightEffect)
            )
        }

        drawCircle(color, center = Offset(nodePos.x, nodePos.y), radius = nodeRadius)

        textLayouts[node]?.let { textLayoutResult ->
            drawText(
                textLayoutResult,
                topLeft =
                    Offset(
                        nodePos.x - textLayoutResult.size.width / 2,
                        nodePos.y - textLayoutResult.size.height / 2
                    )
            )
        }
    }
}

internal suspend fun focusNodeAnimated(
    root: TreeNode,
    pos: Map<TreeNode, NodeLayout>,
    currentStep: Int,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    scale: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val allNodes = mutableSetOf<TreeNode>()
    root.collect(allNodes, currentStep)
    val targetNode = allNodes.find { it.step == currentStep }

    if (targetNode != null) {
        val targetX =
            if (currentStep != 0 && targetNode.parents.size >= 2) {
                (pos[targetNode.parents.first()]!!.x + pos[targetNode.parents.last()]!!.x) / 2
            } else if (currentStep != 0) {
                (
                    pos[targetNode.parents
                            .first()
                            .children
                            .first()]!!.x +
                        pos[targetNode.parents
                                .first()
                                .children
                                .last()]!!.x
                ) / 2
            } else {
                pos[targetNode]!!.x
            }

        val targetY =
            if (currentStep != 0 && targetNode.parents.isNotEmpty()) {
                pos[targetNode.parents.first()]!!.y
            } else {
                pos[targetNode]!!.y
            }

        offsetX.animateTo(canvasWidth / 2f - targetX * scale)
        offsetY.animateTo(canvasHeight / 2f - targetY * scale)
    }
}

private fun DrawScope.drawEdge(
    parentX: Float,
    parentY: Float,
    child: TreeNode,
    pos: Map<TreeNode, NodeLayout>,
    color: Color,
    strokeWidth: Float = 5f
) {
    val childPos = pos[child] ?: return
    if (childPos.depth.isEmpty()) return

    val minDepth = childPos.depth.min()
    val maxDepth = childPos.depth.max()

    drawLine(
        color = color,
        start = Offset(parentX, parentY),
        end = Offset(childPos.x, minDepth),
        strokeWidth = strokeWidth
    )

    if (childPos.y == maxDepth) {
        drawLine(
            color = color,
            start = Offset(childPos.x, minDepth),
            end = Offset(childPos.x, maxDepth),
            strokeWidth = strokeWidth
        )
    }
}
