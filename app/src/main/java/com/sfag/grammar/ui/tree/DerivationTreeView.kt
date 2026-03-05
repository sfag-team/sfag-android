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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withSave
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

import com.sfag.grammar.model.GrammarType

@Composable
fun DerivationView(
    root: DerivationNode,
    scale: MutableState<Float>,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    step: Int,
    type: GrammarType,
    canvasSize: MutableState<Size>
) {
    var allNodes = mutableSetOf<DerivationNode>()

    collect(root, allNodes, step)
    if(type == GrammarType.UNRESTRICTED || type == GrammarType.CONTEXT_SENSITIVE){
        allNodes = allNodes
            .sortedWith(compareBy({ it.depth.max() }, { it.x }))
            .toCollection(LinkedHashSet())
    }
    val colorScheme = MaterialTheme.colorScheme
    val dagNonTerminalColor = colorScheme.primaryContainer
    val dagTerminalColor = colorScheme.tertiaryContainer
    val dagAnimationHighlightColor = colorScheme.tertiary
    val dagGroupingRectColor = colorScheme.outline
    val dagEdgeColor = colorScheme.inverseOnSurface
    val dagNodeTextColor = colorScheme.onPrimaryContainer
    val dagLeafHighlightColor = colorScheme.secondary
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "dash-animation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phase)
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { layoutSize ->
                canvasSize.value = Size(layoutSize.width.toFloat(), layoutSize.height.toFloat())
            }
            .pointerInput(Unit) {
                coroutineScope {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale.value = (scale.value * zoom).coerceIn(0.1f, 5f)
                        launch {
                            offsetX.snapTo(offsetX.value + pan.x)
                            offsetY.snapTo(offsetY.value + pan.y)
                        }
                    }
                }
            }
            .clipToBounds()
    ) {
        with(drawContext.canvas.nativeCanvas) {
            withSave {
                translate(offsetX.value, offsetY.value) // Apply panning
                scale(scale.value, scale.value) // Apply zoom
                drawDAG(
                    nodes = allNodes,
                    size = 40f,
                    textMeasurer = textMeasurer,
                    step = step,
                    highlightEffect = pathEffect,
                    nonTerminalColor = dagNonTerminalColor,
                    terminalColor = dagTerminalColor,
                    animationHighlightColor = dagAnimationHighlightColor,
                    groupingRectColor = dagGroupingRectColor,
                    edgeColor = dagEdgeColor,
                    nodeTextColor = dagNodeTextColor,
                    leafHighlightColor = dagLeafHighlightColor,
                )
            }
        }
    }
}

fun DrawScope.drawDAG(
    nodes: Collection<DerivationNode>,
    size: Float,
    textMeasurer: TextMeasurer,
    step: Int,
    highlightEffect: PathEffect? = null,
    nonTerminalColor: Color,
    terminalColor: Color,
    animationHighlightColor: Color,
    groupingRectColor: Color,
    edgeColor: Color,
    nodeTextColor: Color,
    leafHighlightColor: Color,
) {
    // Final step
    val leafColor = if(step == nodes.maxOf { it.step }+1){
        leafHighlightColor
    }else{
        nodeTextColor
    }
    // Set y-coord
    nodes.forEach { node ->
        node.y = if(node.children.isNotEmpty() && step < node.children.first().step  ){
            node.depth.min()
        }else{
            node.depth.max()
        }
    }
    val visitedChildren = mutableSetOf<DerivationNode>()
    // Draw edges
    nodes.forEach { node ->
        if(node.children.isNotEmpty() && node.children.first().step <= step){
            for(child in node.children){
                if(!visitedChildren.add(child)){
                    continue
                }
                if(child.parents.size > 1){
                    val x = child.parents.map { it.x }.average().toFloat()
                    drawEdgeToChild(x, node.y, child, edgeColor)
                }else{
                    drawEdgeToChild(node.x, node.y, child, edgeColor)
                }
            }
        }

        // Grouping of parents
        if(node.children.isNotEmpty() && node.children.first().parents.size > 1 && step >= node.children.first().step && node.children.first().parents.first() == node){
            drawRoundRect(
                color = groupingRectColor,
                topLeft = Offset(node.x-15-size, node.y-15-size),
                size = Size(node.children.first().parents.last().x+size+15 - (node.x-15-size), node.y+size+15 - (node.y-15-size)),
                cornerRadius = CornerRadius(size, size)
            )
        }

        // Node color - terminal/non-terminal
        val color = if (!node.label.isDigit() && node.label.isUpperCase()) {
            nonTerminalColor
        } else {
            terminalColor
        }

        // Animation
        if(node.children.isNotEmpty() && node.children.first().step == step) {
            drawCircle(
                animationHighlightColor,
                center = Offset(node.x, node.y),
                radius = size,
                style = Stroke(width = 15f, pathEffect = highlightEffect)
            )
        }else if(node.step == step){
            drawCircle(
                nodeTextColor,
                center = Offset(node.x, node.y),
                radius = size,
                style = Stroke(width = 15f, pathEffect = highlightEffect)
            )
        }
        // Draw node circle
        drawCircle(
            color,
            center = Offset(node.x, node.y),
            radius = size,
        )

        // Symbol inside node
        val textLayoutResult = textMeasurer.measure(
            text = node.label.toString(),
            style = TextStyle(
                fontSize = 20.sp,
                color = if(node.children.isEmpty()){ leafColor }else{ nodeTextColor },
                textAlign = TextAlign.Center
            )
        )

        // Draw text centered on the node
        drawText(
            textLayoutResult,
            topLeft = Offset(
                node.x - textLayoutResult.size.width / 2,
                node.y - textLayoutResult.size.height / 2
            )
        )
    }
}

suspend fun focusNodeAnimated(
    root: DerivationNode,
    step: Int,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    scale: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val allNodes = mutableSetOf<DerivationNode>()
    collect(root, allNodes, step)
    val targetNode = allNodes.find { it.step == step }

    if (targetNode != null) {
        val targetX = if (step != 0 && targetNode.parents.size >= 2) {
            (targetNode.parents.first().x + targetNode.parents.last().x) / 2
        } else if(step != 0){
            (targetNode.parents.first().children.first().x + targetNode.parents.first().children.last().x)/2
        }
        else {
            targetNode.x
        }

        val targetY = if (step != 0 && targetNode.parents.isNotEmpty()) {
            targetNode.parents.first().y
        } else {
            targetNode.y
        }

        offsetX.animateTo(canvasWidth / 2f - targetX * scale)
        offsetY.animateTo(canvasHeight / 2f - targetY * scale)
    }
}

fun DrawScope.drawEdgeToChild(
    parentX: Float,
    parentY: Float,
    child: DerivationNode,
    color: Color,
    strokeWidth: Float = 5f
) {
    if (child.depth.isEmpty()) return

    val minDepth = child.depth.min()
    val maxDepth = child.depth.max()

    // Draw from parent to top of child
    drawLine(
        color = color,
        start = Offset(parentX, parentY),
        end = Offset(child.x, minDepth),
        strokeWidth = strokeWidth
    )

    // Draw vertical from min to max depth (if needed)
    if (child.y == maxDepth) {
        drawLine(
            color = color,
            start = Offset(child.x, minDepth),
            end = Offset(child.x, maxDepth),
            strokeWidth = strokeWidth
        )
    }
}
