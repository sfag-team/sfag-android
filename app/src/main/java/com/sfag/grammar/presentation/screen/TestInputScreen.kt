package com.sfag.grammar.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.withSave
import com.sfag.grammar.domain.usecase.parser.Step
import com.sfag.grammar.domain.usecase.parser.parse
import com.sfag.grammar.domain.model.rule.GrammarRule
import com.sfag.grammar.domain.model.type.GrammarType
import com.sfag.grammar.presentation.viewmodel.GrammarViewModel
import com.sfag.shared.util.Symbols
import com.sfag.shared.presentation.icon.ChevronLeft
import com.sfag.shared.presentation.icon.ChevronRight
import com.sfag.shared.presentation.icon.KeyboardDoubleArrowRight
import com.sfag.shared.presentation.icon.NetworkNode
import com.sfag.shared.presentation.theme.light_blue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun TestInputScreen(grammarViewModel: GrammarViewModel, preInput: String) {
    var input by remember { mutableStateOf(if (preInput == ".") "" else preInput) }
    var printInput by remember { mutableStateOf(preInput) }
    val rules = grammarViewModel.getIndividualRules()
    val terminals =  grammarViewModel.terminals.value ?: emptySet()
    val type = grammarViewModel.grammarType.value ?: GrammarType.UNRESTRICTED
    var result by remember { mutableStateOf(parse(preInput, rules, terminals, type)) }
    var parseFlag by remember { mutableStateOf(preInput != ".") }
    var showTable by remember { mutableStateOf(preInput != ".") }
    var showLin by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showTree by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val scale = remember { mutableFloatStateOf(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val canvasSize = remember { mutableStateOf(Size.Zero) }

    val tree = remember(result) {
        result?.let {
            val built = buildTree(it)
            if (type == GrammarType.UNRESTRICTED || type == GrammarType.CONTEXT_SENSITIVE) {
                layoutPrettyTreeUnrestricted(built)
            } else {
                layoutPrettyTreeRestricted(built)
            }
            built
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = "Test an input",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
        )
        HorizontalDivider(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(),

            color = MaterialTheme.colorScheme.primary
        )
        Row(verticalAlignment = Alignment.CenterVertically){
            OutlinedTextField(
                value = input,
                onValueChange = {
                    parseFlag=false
                    input = it
                },
                placeholder = { Text("string containing only terminals") },
                modifier = Modifier.weight(1f).padding(top = 4.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = light_blue,
                    focusedContainerColor = light_blue
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    printInput = input
                    showTree = false
                    showTable = false
                    showLin = false
                    focusManager.clearFocus()
                    coroutineScope.launch {
                        result = parse(input, rules, terminals, type)
                        parseFlag = true // Parsing complete
                    }
                },
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Check input", tint=MaterialTheme.colorScheme.primary)
            }

        }
        if(parseFlag){
            result.let {
                if (showTree) {
                    var steps by remember { mutableIntStateOf(0) }
                    if(it != null && tree != null){
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent background
                                .zIndex(1f) // Ensure it's on top

                        ) {
                            DAGCanvas(tree, scale, offsetX, offsetY, steps, type, canvasSize)
                            if(steps == 0){
                                coroutineScope.launch {
                                    focusNodeAnimated(tree, steps, offsetX, offsetY, scale.floatValue, canvasSize.value.width, canvasSize.value.height)
                                }
                            }
                            if (steps == 1 + it.size) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(2f)// Ensure it's on top
                                        .padding(bottom = 64.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally){
                                        Text(
                                            text = "Derivation completed",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(text = input,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Yellow,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { showTree = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close tree")
                            }
                            IconButton(
                                onClick = {
                                    steps = 0
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                            }
                            IconButton(
                                onClick = {
                                    if (steps < it.size) {
                                        coroutineScope.launch {
                                            steps++
                                            focusNodeAnimated(tree, steps, offsetX, offsetY, scale.floatValue, canvasSize.value.width, canvasSize.value.height)
                                        }
                                    }else if(steps == it.size){
                                        steps++
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 30.dp, bottom = 4.dp)
                            ) {
                                Icon(ChevronRight, contentDescription = "Next step")
                            }
                            IconButton(
                                onClick = {
                                    if (steps > 0) {
                                        coroutineScope.launch {
                                            steps--
                                            focusNodeAnimated(tree, steps, offsetX, offsetY, scale.floatValue, canvasSize.value.width, canvasSize.value.height)
                                        }}
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 30.dp, bottom = 4.dp)
                            ) {
                                Icon(ChevronLeft, contentDescription = "Previous step")
                            }
                        }
                    }
                }
                //**********
                // Input accepted/notaccepted
                //**********
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                ){

                    if (it != null){
                        Text(text =  buildAnnotatedString{
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontSize = 20.sp)) {
                                append("\"$printInput\"")
                            }
                            append(" is accepted")
                        },
                            modifier = Modifier
                                .weight(1f)
                                .background(color = Color(0xFF38F292)),
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }else{
                        Text(text =  buildAnnotatedString{
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontSize = 20.sp)) {
                                append("\"$printInput\"")
                            }
                            append(" is not accepted")
                        },
                            modifier = Modifier
                                .weight(1f)
                                .background(color = Color(0xFFDB5C65)),
                            textAlign = TextAlign.Center
                        )

                    }
                }
                if(it != null){
                    //***********
                    //  Visual options
                    //***********

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                        Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally))
                    {
                        IconButton(
                            onClick = {
                                showTable = false
                                showLin = true}
                        ){
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Linear button", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                showLin = false
                                showTable = true}
                        ) {
                            Icon(Icons.Default.Menu, "Table button", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                showTree = true

                            }
                        ){
                            Icon(NetworkNode, "Tree button", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if(showTable){
                        StateTable(it)
                    }
                    if(showLin){
                        LinearDerivation(it)
                    }


                }
            }
        }
        if(!parseFlag || result == null){
            Text(
                text = "Rules",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp)
            )
            LazyColumn {
                items(rules){ rule->
                    DisplayRule(
                        rule = rule,
                        grammarViewModel = grammarViewModel,
                        grammarViewModel.isGrammarFinished.value == true
                    ) { Unit }
                }
            }
        }
    }


}

@Composable
fun LinearDerivation(steps: List<Step>) {
    Box(
        modifier = Modifier
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
            .padding(8.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center

    ) {
        Text( // ε handling in derivation
            text = "S => " + steps.joinToString(" => ") {
                if (it.stateString == Symbols.EPSILON) Symbols.EPSILON else it.stateString.replace(Symbols.EPSILON, "")
            }
        )
    }

}

@Composable
fun StateTable(steps: List<Step>) {
    var step by remember { mutableIntStateOf(1) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
    ) {
        // Table Header
        Row(modifier = Modifier.padding(horizontal = 6.dp)) {
            IconButton(
                onClick = { if (step > 1) step-- } // Lower bound
            ) {
                Icon(ChevronLeft, contentDescription = "Previous step")
            }
            IconButton(
                onClick = { if (step < steps.size) step++ } // Upper bound
            ) {
                Icon(ChevronRight, contentDescription = "Next step")
            }
            IconButton(
                onClick = { step = steps.size } // Jump to the end
            ) {
                Icon(KeyboardDoubleArrowRight, contentDescription = "Jump to end")
            }
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
        ){
            Text(
                text = "Applied Rule",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "String",
                modifier = Modifier.weight(2f),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider()

        // Table Rows
        LazyColumn {
            items(steps.take(step)) { state ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)) {

                    val diffIndex = findDifferenceIndex(state.appliedRule, state.previous, state.stateString)

                    val right = if(state.appliedRule.right == Symbols.EPSILON){ // ε
                        ""
                    }else{
                        state.appliedRule.right.replace(Symbols.EPSILON, "")
                    }

                    // Build annotated string for displaying with different colors
                    val annotatedString = buildAnnotatedString {
                        // Unchanged part
                        append(state.previous.replace(Symbols.EPSILON, "").take(diffIndex))

                        // Changed part with different color
                        withStyle(style = SpanStyle(color = Color(0xFF2779C2))) {
                            append(right)
                        }
                        // The rest of the baseString
                        append(state.previous.replace(Symbols.EPSILON, "").drop(diffIndex + state.appliedRule.left.length))
                    }

                    Text(
                        text = state.appliedRule.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = annotatedString,
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

fun findDifferenceIndex(rule: GrammarRule, previous: String, current: String): Int {
    val left = rule.left
    for (i in previous.indices) {
        if (i + left.length <= previous.length && previous.substring(i, i + left.length) == left) {
            val candidate = previous.substring(0, i) + rule.right + previous.substring(i + left.length)
            if (candidate == current) {
                return i
            }
        }
    }
    return -1
}

fun buildTree(steps: List<Step>): DAGNode {
    // Init first step
    val root = DAGNode(0, 'S', LinkedHashSet(), LinkedHashSet())
    var currentLeaves = mutableListOf(root)

    for ((stepIdx, step) in steps.withIndex()) {
        // Find replacement position in previous state
        val replacePos = findDifferenceIndex(step.appliedRule, step.previous, step.stateString)
        // Create new child nodes
        val newChildren = step.appliedRule.right.mapTo(LinkedHashSet()) { symbol ->
            DAGNode(stepIdx + 1, symbol, LinkedHashSet(), LinkedHashSet())
        }

        for(i in 0..< step.appliedRule.left.length){
            val targetNode = currentLeaves[replacePos+i]
            for (c in newChildren) {
                targetNode.addChild(c)
            }
        }
        currentLeaves = getCurrentLeaves(root)
    }
    return root
}


private fun getCurrentLeaves(node: DAGNode): MutableList<DAGNode> {
    val leaves = LinkedHashSet<DAGNode>()
    fun traverse(n: DAGNode) {
        if (n.children.isEmpty()) {
            leaves.add(n)
        } else {
            n.children.forEach(::traverse)
        }
    }
    traverse(node)
    return leaves.toMutableList()
}


class DAGNode(
    val step: Int,
    val label: Char,
    val parents: MutableSet<DAGNode> = mutableSetOf(),
    val children: MutableSet<DAGNode> = mutableSetOf(),
) {
    fun addChild(child: DAGNode) {
        children.add(child)
        child.parents.add(this)
    }
    override fun toString(): String {
        return label.toString()
    }
    // Layout
    var x: Float = 1f
    var y: Float = 0f
    val depth: MutableSet<Float> = LinkedHashSet()
}

fun layoutPrettyTreeUnrestricted(root: DAGNode, nodeSpacing: Float = 120f, layerHeight: Float = 150f){
    var nextX = 0f

    // Positioning
    fun firstWalk(node: DAGNode, depth: Int = 0, visited: MutableSet<DAGNode> = mutableSetOf()) {
        if (!visited.add(node)){
            val maxParentDepth = node.parents.flatMap { it.depth }.maxOrNull()
            if(maxParentDepth == null){
                return
            }else{
                node.depth.clear()
                node.depth.add(maxParentDepth +layerHeight)
            }
            return
        }
        node.depth.add(depth*layerHeight)

        val children = node.children
        // Set x for leave
        if(children.isEmpty()){
            if(node.x == 1f){
                node.x = nextX
                nextX += nodeSpacing
            }
        }else{
            children.forEach{firstWalk(it, depth+1, visited)}

            val left = children.first()
            val right = children.last()
            val parentsSize = left.parents.size
            // Move children if they have more parents and create space for them
            if(children.size < parentsSize && left.parents.first() == node){
                val space = (parentsSize-1)*nodeSpacing
                val childrenSpace = (children.size-1) * nodeSpacing
                var start =(space-childrenSpace)/2f
                for(it in children){
                    it.x += start
                    start += nodeSpacing
                }
            }
            //Center the parents
            // Single parent
            if(left.parents.size == 1){
                node.x = (left.x + right.x) / 2
            } else{
                // Multiple parent
                val parentCount = right.parents.size
                val nodeIndex = right.parents.indexOf(node)
                val centerOffset = (parentCount - 1) / 2f
                val offset = (nodeIndex - centerOffset) * nodeSpacing
                node.x = (left.x + right.x) / 2f + offset
            }
        }

    }
    firstWalk(root)
    // Set heights
    fun secondWalk(node: DAGNode, layerHeight: Float, visited: MutableSet<DAGNode> = mutableSetOf()) {
        if (!visited.add(node)) return  // Skip if already visited

        node.children.forEach { secondWalk(it, layerHeight, visited) }
        if (node.children.isNotEmpty()) {
            val commonDepth = node.children
                .map { it.depth }
                .reduceOrNull { acc, set -> acc.intersect(set).toMutableSet() }
                ?.maxOrNull()

            if (commonDepth != null) {
                node.depth.add(commonDepth - layerHeight)
            }
        }
    }
    secondWalk(root, layerHeight)
}

fun layoutPrettyTreeRestricted(root: DAGNode, nodeSpacing: Float = 100f, layerHeight: Float = 150f) {
    var nextX = 0f

    fun firstWalk(node: DAGNode, depth: Int = 0) {
        node.depth.add(depth*layerHeight)

        val children = node.children
        if (children.isEmpty()) {
            node.x = nextX
            nextX += nodeSpacing
        } else {
            children.forEach { firstWalk(it, depth + 1) }
            // Center parrent
            val left = children.first()
            val right = children.last()
            node.x = (left.x + right.x) / 2f
        }
    }
    firstWalk(root)
}

fun collect(node: DAGNode, nodes: MutableSet<DAGNode>, step: Int) {
    if(node.step > step) return
    if (nodes.add(node)) {
        node.children.forEach { collect(it, nodes, step) }
    }
}

@Composable
fun DAGCanvas
            (
    root: DAGNode,
    scale: MutableState<Float>,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    step: Int,
    type: GrammarType,
    canvasSize: MutableState<Size>
)
{
    var allNodes = mutableSetOf<DAGNode>()

    collect(root, allNodes, step)
    if(type == GrammarType.UNRESTRICTED || type == GrammarType.CONTEXT_SENSITIVE){
        allNodes = allNodes
            .sortedWith(compareBy({ it.depth.max() }, { it.x }))
            .toCollection(LinkedHashSet())

    }
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
                drawDAG(allNodes, 40f, textMeasurer, step, pathEffect)
            }
        }
    }

}

fun DrawScope.drawDAG(nodes: Collection<DAGNode>, size: Float, textMeasurer: TextMeasurer, step: Int, highlightEffect: PathEffect? = null) {
    // Final step
    val leafColor = if(step == nodes.maxOf { it.step }+1){
        Color.Yellow
    }else{
        Color.White
    }
    // Set y-coord
    nodes.forEach { node ->
        node.y = if(node.children.isNotEmpty() && step < node.children.first().step  ){
            node.depth.min()
        }else{
            node.depth.max()
        }
    }
    val visitedChildren = mutableSetOf<DAGNode>()
    // Draw edges
    nodes.forEach { node ->
        if(node.children.isNotEmpty() && node.children.first().step <= step){
            for(child in node.children){
                if(!visitedChildren.add(child)){
                    continue
                }
                if(child.parents.size > 1){
                    val x = child.parents.map { it.x }.average().toFloat()
                    drawEdgeToChild(x, node.y, child)
                }else{
                    drawEdgeToChild(node.x, node.y, child)
                }
            }
        }

        // Grouping of parents
        if(node.children.isNotEmpty() && node.children.first().parents.size > 1 && step >= node.children.first().step && node.children.first().parents.first() == node){
            drawRoundRect(
                color = Color(0xff7bc2ed),
                topLeft = Offset(node.x-15-size, node.y-15-size),
                size = Size(node.children.first().parents.last().x+size+15 - (node.x-15-size), node.y+size+15 - (node.y-15-size)),
                cornerRadius = CornerRadius(size, size)
            )
        }

        // Node color - terminal/non-terminal
        val color = if (!node.label.isDigit() && node.label.isUpperCase()) {
            Color(0xff77e681)
        } else {
            Color(0xff0479c2)
        }

        // Animation
        if(node.children.isNotEmpty() && node.children.first().step == step) {
            drawCircle(
                Color(0xfff5823b),
                center = Offset(node.x, node.y),
                radius = size,
                style = Stroke(width = 15f, pathEffect = highlightEffect)
            )
        }else if(node.step == step){
            drawCircle(
                Color.White,
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
                color = if(node.children.isEmpty()){ leafColor }else{Color.White},
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
    root: DAGNode,
    step: Int,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    scale: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val allNodes = mutableSetOf<DAGNode>()
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
    child: DAGNode,
    color: Color = Color.White,
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
