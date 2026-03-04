package com.sfag.automata.ui.component.visualization

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.tree.NodeStatus
import com.sfag.automata.domain.model.tree.TreeNode
import com.sfag.automata.domain.usecase.isDeterministic
import com.sfag.automata.ui.component.TREE_EDGE_LINE_WIDTH
import com.sfag.automata.ui.component.TREE_PADDING
import com.sfag.automata.ui.component.TREE_NODE_RADIUS
import com.sfag.automata.ui.component.TREE_NODE_OUTLINE_WIDTH
import com.sfag.automata.ui.component.TREE_NODE_TEXT_SIZE
import com.sfag.shared.ui.theme.extendedColorScheme

@Composable
fun Machine.DerivationTree(recomposeKey: Int = 0) {
    if (machineType != MachineType.Finite || isDeterministic() != false) return
    if (derivationTree.root == null) {
        val initialState = states.firstOrNull { it.initial }
        if (initialState != null) {
            derivationTree.initialize(initialState.name)
        } else {
            return
        }
    }

    val density = LocalDensity.current

    val colorActiveFill = MaterialTheme.colorScheme.surfaceContainerLowest
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val colorBg = MaterialTheme.colorScheme.surfaceContainer
    val colorAcceptedFill = MaterialTheme.extendedColorScheme.accepted.colorContainer
    val colorAcceptedText = MaterialTheme.extendedColorScheme.accepted.onColorContainer
    val colorRejectedFill = MaterialTheme.extendedColorScheme.rejected.colorContainer
    val colorRejectedText = MaterialTheme.extendedColorScheme.rejected.onColorContainer

    val textPaint = remember(density) {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            textSize = TREE_NODE_TEXT_SIZE * density.density
        }
    }

    val positions = remember(recomposeKey) {
        calculateLayout(derivationTree)
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var userHasDragged by remember(recomposeKey) { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    // Animate newly added generation of nodes
    val newNodeAlpha = remember { Animatable(1f) }
    val latestAnimatedGen = remember { mutableIntStateOf(0) }
    LaunchedEffect(recomposeKey) {
        val gen = derivationTree.getCurrentGeneration()
        if (gen > latestAnimatedGen.intValue) {
            latestAnimatedGen.intValue = gen
            newNodeAlpha.snapTo(0f)
            newNodeAlpha.animateTo(1f, tween(400))
        }
    }

    // Auto-center + auto-zoom on current active nodes
    LaunchedEffect(recomposeKey) {
        if (userHasDragged) return@LaunchedEffect
        val active = derivationTree.getActiveNodes()
        val activePositions = active.mapNotNull { positions[it.id] }
        if (activePositions.isEmpty()) return@LaunchedEffect

        val centroidX = activePositions.map { it.x }.average().toFloat()
        val centroidY = activePositions.map { it.y }.average().toFloat()

        val centroidXPx = with(density) { centroidX.dp.toPx() }
        val centroidYPx = with(density) { centroidY.dp.toPx() }

        val minY = activePositions.minOf { it.y }
        val maxY = activePositions.maxOf { it.y }
        val spanPx = with(density) { (maxY - minY + TREE_PADDING * 2).dp.toPx() }
        val viewHeightPx = with(density) { 300.dp.toPx() }

        scale = if (spanPx > viewHeightPx && spanPx > 0f) {
            (viewHeightPx / spanPx).coerceIn(0.3f, 1f)
        } else {
            1f
        }

        if (initialized && canvasWidthPx > 0f) {
            offsetX = canvasWidthPx / 2f - centroidXPx * scale
            offsetY = viewHeightPx / 2f - centroidYPx * scale
        }
        initialized = true
    }

    val currentGen = derivationTree.getCurrentGeneration()
    val alphaValue = newNodeAlpha.value

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .onSizeChanged { canvasWidthPx = it.width.toFloat() }
            .clip(MaterialTheme.shapes.medium)
            .background(colorBg)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(0.3f, 3f)
                    offsetX += (centroid.x - offsetX) * (1 - scale / oldScale) + pan.x
                    offsetY += (centroid.y - offsetY) * (1 - scale / oldScale) + pan.y
                    userHasDragged = true
                }
            }
    ) {
        if (!userHasDragged && !initialized) {
            val rootPos = positions[derivationTree.root?.id ?: 0]
            if (rootPos != null) {
                val rootXPx = rootPos.x.dp.toPx()
                val rootYPx = rootPos.y.dp.toPx()
                offsetX = size.width / 2f - rootXPx * scale
                offsetY = size.height / 2f - rootYPx * scale
            }
        }

        // Edges always neutral
        fun edgeColor(child: TreeNode): Color = colorOnSurface

        // For nodes: only color leaves; interior nodes always look like ACTIVE
        fun effectiveStatus(node: TreeNode): NodeStatus =
            if (node.children.isEmpty()) node.status else NodeStatus.ACTIVE

        fun fillColor(node: TreeNode): Color = when (effectiveStatus(node)) {
            NodeStatus.ACTIVE -> colorActiveFill
            NodeStatus.REJECTED -> colorRejectedFill
            NodeStatus.ACCEPTED -> colorAcceptedFill
        }

        // Outlines always neutral
        fun borderColor(node: TreeNode): Color = colorOnSurface

        fun textColor(node: TreeNode): Int = when (effectiveStatus(node)) {
            NodeStatus.ACTIVE -> colorOnSurface.toArgb()
            NodeStatus.REJECTED -> colorRejectedText.toArgb()
            NodeStatus.ACCEPTED -> colorAcceptedText.toArgb()
        }

        translate(left = offsetX, top = offsetY) {
            scale(scale = scale, pivot = Offset.Zero) {
                fun drawEdges(node: TreeNode) {
                    val parentPos = positions[node.id] ?: return
                    val parentWorld = Offset(parentPos.x.dp.toPx(), parentPos.y.dp.toPx())
                    for (child in node.children) {
                        val childPos = positions[child.id] ?: continue
                        val childWorld = Offset(childPos.x.dp.toPx(), childPos.y.dp.toPx())
                        val edgeAlpha = if (child.generation == currentGen && currentGen > 0) alphaValue else 1f
                        drawLine(
                            color = edgeColor(child),
                            start = Offset(parentWorld.x + TREE_NODE_RADIUS, parentWorld.y),
                            end = Offset(childWorld.x - TREE_NODE_RADIUS, childWorld.y),
                            strokeWidth = TREE_EDGE_LINE_WIDTH,
                            alpha = edgeAlpha
                        )
                        drawEdges(child)
                    }
                }

                val root = derivationTree.root
                if (root != null) {
                    drawEdges(root)
                }

                fun drawNodes(node: TreeNode) {
                    val pos = positions[node.id] ?: return
                    val world = Offset(pos.x.dp.toPx(), pos.y.dp.toPx())
                    val nodeAlpha = if (node.generation == currentGen && currentGen > 0) alphaValue else 1f

                    drawCircle(
                        color = borderColor(node),
                        radius = TREE_NODE_RADIUS,
                        center = world,
                        style = Stroke(width = TREE_NODE_OUTLINE_WIDTH),
                        alpha = nodeAlpha
                    )
                    drawCircle(
                        color = fillColor(node),
                        radius = TREE_NODE_RADIUS,
                        center = world,
                        alpha = nodeAlpha
                    )
                    node.stateName?.let { name ->
                        textPaint.color = textColor(node)
                        textPaint.alpha = (nodeAlpha * 255).toInt()
                        drawContext.canvas.nativeCanvas.drawText(
                            name,
                            world.x,
                            world.y + textPaint.textSize / 3f,
                            textPaint
                        )
                    }

                    for (child in node.children) {
                        drawNodes(child)
                    }
                }

                if (root != null) {
                    drawNodes(root)
                }
            }
        }
    }
}
