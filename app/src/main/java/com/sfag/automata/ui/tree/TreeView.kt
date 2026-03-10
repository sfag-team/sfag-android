package com.sfag.automata.ui.tree

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.common.checkDeterminism
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.tree.TreeNode
import com.sfag.automata.ui.common.EDGE_LINE_WIDTH
import com.sfag.automata.ui.common.NODE_OUTLINE_WIDTH
import com.sfag.automata.ui.common.NODE_RADIUS
import com.sfag.automata.ui.common.NODE_TEXT_SIZE
import com.sfag.automata.ui.common.TREE_CANVAS_HEIGHT
import com.sfag.main.config.MANUAL_MAX_ZOOM
import com.sfag.main.config.MANUAL_MIN_ZOOM
import com.sfag.main.ui.theme.extendedColorScheme

@Composable
fun Machine.TreeView(
    recomposeKey: Int,
    onSelectNode: ((Int) -> Unit)?,
) {
    if (checkDeterminism() != false) return
    if (tree.root == null) {
        val initialState = states.firstOrNull { it.initial }
        if (initialState != null) {
            tree.initialize(initialState.name)
        } else {
            return
        }
    }

    val density = LocalDensity.current

    // Captured here because MaterialTheme is a @Composable accessor unavailable in DrawScope
    val colors = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColorScheme

    val textPaint =
        remember(density) {
            Paint().apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
                isAntiAlias = true
                textSize = NODE_TEXT_SIZE * density.density
            }
        }

    val badgePaint =
        remember(density, colors.onSurface) {
            Paint().apply {
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                textSize = NODE_TEXT_SIZE * density.density
                color = colors.onSurface.toArgb()
            }
        }

    // PDA node selection - only PushdownMachine tracks which tree node the user tapped
    val selectedId = (this@TreeView as? PushdownMachine)?.selectedNodeId

    val positions = remember(recomposeKey) { calculateLayout(tree) }

    // Pre-compute dp-to-px positions to avoid per-node conversion every frame
    val positionsPx =
        remember(positions, density) {
            positions.mapValues { (_, offset) ->
                with(density) { Offset(offset.x.dp.toPx(), offset.y.dp.toPx()) }
            }
        }

    var scale by remember { mutableFloatStateOf(0.75f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var userHasDragged by remember { mutableStateOf(false) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    var canvasHeightPx by remember { mutableFloatStateOf(0f) }

    // Reset drag/center state on new machine load
    val lastCenteredGen = remember { mutableIntStateOf(-1) }
    LaunchedEffect(this@TreeView) {
        userHasDragged = false
        lastCenteredGen.intValue = -1
    }

    // Bounds for viewport clamping - snapshot state so gesture handler reads latest
    var treeBounds by remember { mutableStateOf<Rect?>(null) }
    SideEffect {
        treeBounds =
            if (positionsPx.isNotEmpty()) {
                Rect(
                    left = positionsPx.values.minOf { it.x },
                    top = positionsPx.values.minOf { it.y },
                    right = positionsPx.values.maxOf { it.x },
                    bottom = positionsPx.values.maxOf { it.y },
                )
            } else {
                null
            }
    }

    // Auto-center on step advance, reset, and initial render
    LaunchedEffect(recomposeKey, canvasWidthPx) {
        if (canvasWidthPx <= 0f) return@LaunchedEffect

        val gen = tree.getCurrentGeneration()
        val viewHeightPx = with(density) { TREE_CANVAS_HEIGHT.toPx() }

        // Reset detected - clear drag flag so auto-center runs
        if (gen < lastCenteredGen.intValue) userHasDragged = false

        // Same generation as last centered - nothing to do
        val isNew = gen != lastCenteredGen.intValue || lastCenteredGen.intValue < 0
        lastCenteredGen.intValue = gen
        if (!isNew) return@LaunchedEffect
        if (userHasDragged) return@LaunchedEffect

        // Center on selected node (PDA stack selection) or active nodes centroid
        val targetPx = selectedId?.let { positionsPx[it] }
        if (targetPx != null) {
            offsetX = canvasWidthPx / 2f - targetPx.x * scale
            offsetY = viewHeightPx / 2f - targetPx.y * scale
        } else {
            val active = tree.getActiveNodes()
            val activePosPx = active.mapNotNull { positionsPx[it.id] }
            if (activePosPx.isEmpty()) return@LaunchedEffect

            val centroidXPx = activePosPx.map { it.x.toDouble() }.average().toFloat()
            val centroidYPx = activePosPx.map { it.y.toDouble() }.average().toFloat()

            offsetX = canvasWidthPx / 2f - centroidXPx * scale
            offsetY = viewHeightPx / 2f - centroidYPx * scale
        }
    }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(TREE_CANVAS_HEIGHT)
                .onSizeChanged {
                    canvasWidthPx = it.width.toFloat()
                    canvasHeightPx = it.height.toFloat()
                }.clip(MaterialTheme.shapes.medium)
                .background(colors.surfaceContainer)
                .pointerInput(onSelectNode, positions) {
                    if (onSelectNode == null) return@pointerInput
                    detectTapGestures { tapOffset ->
                        // Convert screen position to tree position
                        val treeX = (tapOffset.x - offsetX) / scale
                        val treeY = (tapOffset.y - offsetY) / scale
                        val active = tree.getActiveNodes()
                        for (node in active) {
                            if (node.children.isNotEmpty()) continue
                            val nodePositionPx = positionsPx[node.id] ?: continue
                            val dx = treeX - nodePositionPx.x
                            val dy = treeY - nodePositionPx.y
                            val hitRadius = NODE_RADIUS + NODE_OUTLINE_WIDTH / 2f
                            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                                onSelectNode(node.id)
                                return@detectTapGestures
                            }
                        }
                    }
                }.pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(MANUAL_MIN_ZOOM, MANUAL_MAX_ZOOM)
                        offsetX += (centroid.x - offsetX) * (1 - scale / oldScale) + pan.x
                        offsetY += (centroid.y - offsetY) * (1 - scale / oldScale) + pan.y
                        // Clamp so at least one node stays partially visible
                        val bounds = treeBounds
                        if (bounds != null && canvasWidthPx > 0f && canvasHeightPx > 0f) {
                            offsetX =
                                offsetX.coerceIn(
                                    -(bounds.right + NODE_RADIUS) * scale,
                                    canvasWidthPx - (bounds.left - NODE_RADIUS) * scale,
                                )
                            offsetY =
                                offsetY.coerceIn(
                                    -(bounds.bottom + NODE_RADIUS) * scale,
                                    canvasHeightPx - (bounds.top - NODE_RADIUS) * scale,
                                )
                        }
                        userHasDragged = true
                    }
                },
    ) {
        // Leaf nodes: active = primary, accepted = green, rejected = red
        // Dead-end nodes use same colors but dimmed via alpha
        // Interior nodes stay neutral (surface fill)
        val deadAlpha = 0.38f

        fun fillColor(node: TreeNode): Color =
            when (node.status) {
                SimulationOutcome.ACTIVE if node.children.isEmpty() ->
                    colors.primaryContainer
                SimulationOutcome.ACCEPTED ->
                    extendedColors.accepted.colorContainer
                SimulationOutcome.REJECTED ->
                    extendedColors.rejected.colorContainer
                else -> colors.surfaceContainerLowest
            }

        fun textColor(node: TreeNode): Int =
            when (node.status) {
                SimulationOutcome.ACTIVE if node.children.isEmpty() ->
                    colors.onPrimaryContainer.toArgb()
                SimulationOutcome.ACCEPTED ->
                    extendedColors.accepted.onColorContainer.toArgb()
                SimulationOutcome.REJECTED ->
                    extendedColors.rejected.onColorContainer.toArgb()
                else -> colors.onSurface.toArgb()
            }

        fun nodeAlpha(node: TreeNode): Float = if (node.status == SimulationOutcome.DEAD) deadAlpha else 1f

        translate(left = offsetX, top = offsetY) {
            scale(scale = scale, pivot = Offset.Zero) {
                fun drawEdges(node: TreeNode) {
                    val parentWorld = positionsPx[node.id] ?: return
                    for (child in node.children) {
                        val childWorld = positionsPx[child.id] ?: continue
                        drawLine(
                            color = colors.onSurface,
                            start = Offset(parentWorld.x + NODE_RADIUS, parentWorld.y),
                            end = Offset(childWorld.x - NODE_RADIUS, childWorld.y),
                            strokeWidth = EDGE_LINE_WIDTH,
                            alpha = nodeAlpha(child),
                        )
                        drawEdges(child)
                    }
                }

                val root = tree.root
                if (root != null) {
                    drawEdges(root)
                }

                fun drawNodes(node: TreeNode) {
                    val world = positionsPx[node.id] ?: return
                    val alpha = nodeAlpha(node)
                    drawCircle(
                        color = colors.onSurface,
                        radius = NODE_RADIUS,
                        center = world,
                        style = Stroke(width = NODE_OUTLINE_WIDTH),
                        alpha = alpha,
                    )
                    drawCircle(
                        color = fillColor(node),
                        radius = NODE_RADIUS,
                        center = world,
                        alpha = alpha,
                    )
                    node.stateName?.let { name ->
                        textPaint.color = textColor(node)
                        textPaint.alpha = (alpha * 255).toInt()
                        drawContext.canvas.nativeCanvas.drawText(
                            name,
                            world.x,
                            world.y + textPaint.textSize / 3f,
                            textPaint,
                        )
                    }

                    // Draw "S" badge outside the top-right of the selected node
                    if (selectedId != null && node.id == selectedId) {
                        val badgeX = world.x + NODE_RADIUS + NODE_OUTLINE_WIDTH
                        val badgeY = world.y - NODE_RADIUS + badgePaint.textSize / 3f
                        drawContext.canvas.nativeCanvas.drawText("S", badgeX, badgeY, badgePaint)
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
