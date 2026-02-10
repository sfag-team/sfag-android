package com.sfag.automata.presentation.component.visualization

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.sfag.automata.domain.usecase.isDeterministicFinite
import com.sfag.automata.presentation.component.TREE_EDGE_LINE_WIDTH
import com.sfag.automata.presentation.component.TREE_PADDING
import com.sfag.automata.presentation.component.TREE_NODE_RADIUS
import com.sfag.automata.presentation.component.TREE_NODE_OUTLINE_WIDTH
import com.sfag.automata.presentation.component.TREE_NODE_TEXT_SIZE

@Composable
fun Machine.DerivationTree(recomposeKey: Int = 0) {
    if (machineType != MachineType.Finite || isDeterministicFinite()) return
    if (derivationTree.root == null) {
        val initialState = states.firstOrNull { it.initial }
        if (initialState != null) {
            derivationTree.initialize(initialState.name)
        } else {
            return
        }
    }

    val density = LocalDensity.current

    val colorActiveFill = MaterialTheme.colorScheme.surface
    val colorRejectedFill = MaterialTheme.colorScheme.surfaceVariant
    val colorAcceptedFill = MaterialTheme.colorScheme.primaryContainer
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colorGray = MaterialTheme.colorScheme.outline
    val colorBg = MaterialTheme.colorScheme.surface

    val textPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
    }
    textPaint.textSize = TREE_NODE_TEXT_SIZE * density.density

    val positions = remember(recomposeKey) {
        calculateLayout(derivationTree)
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var userHasDragged by remember(recomposeKey) { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    // Auto-center + auto-zoom on frontier
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

        // Only auto-center after first expansion (not on init)
        if (initialized && canvasWidthPx > 0f) {
            offsetX = canvasWidthPx / 2f - centroidXPx * scale
            offsetY = viewHeightPx / 2f - centroidYPx * scale
        }
        initialized = true
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .onSizeChanged { canvasWidthPx = it.width.toFloat() }
            .clip(MaterialTheme.shapes.large)
            .border(2.dp, colorTertiary, MaterialTheme.shapes.large)
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
        // If not yet auto-centered, center on root
        if (!userHasDragged && !initialized) {
            val rootPos = positions[derivationTree.root?.id ?: 0]
            if (rootPos != null) {
                val rootXPx = rootPos.x.dp.toPx()
                val rootYPx = rootPos.y.dp.toPx()
                offsetX = size.width / 2f - rootXPx * scale
                offsetY = size.height / 2f - rootYPx * scale
            }
        }

        fun edgeColor(child: TreeNode): Color = when (child.status) {
            NodeStatus.REJECTED -> colorGray
            NodeStatus.ACCEPTED -> colorAcceptedFill
            NodeStatus.ACTIVE -> colorTertiary
        }

        fun fillColor(status: NodeStatus): Color = when (status) {
            NodeStatus.ACTIVE -> colorActiveFill
            NodeStatus.REJECTED -> colorRejectedFill
            NodeStatus.ACCEPTED -> colorAcceptedFill
        }

        fun borderColor(status: NodeStatus): Color = when (status) {
            NodeStatus.REJECTED -> colorGray
            NodeStatus.ACTIVE -> colorTertiary
            NodeStatus.ACCEPTED -> colorTertiary
        }

        fun textColor(status: NodeStatus): Int = when (status) {
            NodeStatus.ACTIVE -> colorTertiary.toArgb()
            NodeStatus.REJECTED -> colorGray.toArgb()
            NodeStatus.ACCEPTED -> colorTertiary.toArgb()
        }

        translate(left = offsetX, top = offsetY) {
            scale(scale = scale, pivot = Offset.Zero) {
                // Draw edges
                fun drawEdges(node: TreeNode) {
                    val parentPos = positions[node.id] ?: return
                    val parentWorld = Offset(parentPos.x.dp.toPx(), parentPos.y.dp.toPx())
                    for (child in node.children) {
                        val childPos = positions[child.id] ?: continue
                        val childWorld = Offset(childPos.x.dp.toPx(), childPos.y.dp.toPx())
                        drawLine(
                            color = edgeColor(child),
                            start = Offset(parentWorld.x + TREE_NODE_RADIUS, parentWorld.y),
                            end = Offset(childWorld.x - TREE_NODE_RADIUS, childWorld.y),
                            strokeWidth = TREE_EDGE_LINE_WIDTH
                        )
                        drawEdges(child)
                    }
                }

                val root = derivationTree.root
                if (root != null) {
                    drawEdges(root)
                }

                // Draw nodes
                fun drawNodes(node: TreeNode) {
                    val pos = positions[node.id] ?: return
                    val world = Offset(pos.x.dp.toPx(), pos.y.dp.toPx())

                    // Border
                    drawCircle(
                        color = borderColor(node.status),
                        radius = TREE_NODE_RADIUS,
                        center = world,
                        style = Stroke(width = TREE_NODE_OUTLINE_WIDTH)
                    )
                    // Fill (drawn on top, covers inner half of stroke)
                    drawCircle(
                        color = fillColor(node.status),
                        radius = TREE_NODE_RADIUS,
                        center = world
                    )
                    // Text
                    node.stateName?.let { name ->
                        textPaint.color = textColor(node.status)
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
