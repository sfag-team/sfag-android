package com.sfag.grammar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

import com.sfag.grammar.model.GrammarRule
import com.sfag.grammar.model.GrammarType
import com.sfag.grammar.model.Step
import com.sfag.grammar.model.parse
import com.sfag.grammar.ui.tree.DerivationView
import com.sfag.grammar.ui.tree.buildTree
import com.sfag.grammar.ui.tree.focusNodeAnimated
import com.sfag.grammar.ui.tree.layoutPrettyTreeRestricted
import com.sfag.grammar.ui.tree.layoutPrettyTreeUnrestricted
import com.sfag.shared.Symbols
import com.sfag.shared.ui.component.ChevronLeft
import com.sfag.shared.ui.component.ChevronRight
import com.sfag.shared.ui.component.KeyboardDoubleArrowRight
import com.sfag.shared.ui.component.NetworkNode
import com.sfag.shared.ui.theme.extendedColorScheme

@Composable
fun TestInputScreen(grammarViewModel: GrammarViewModel, preInput: String) {
    var input by remember { mutableStateOf(if (preInput == ".") "" else preInput) }
    var printInput by remember { mutableStateOf(preInput) }
    val rules = grammarViewModel.getIndividualRules()
    val terminals =  grammarViewModel.terminals
    val type = grammarViewModel.grammarType
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
                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
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
                                .background(MaterialTheme.colorScheme.surfaceDim)
                                .zIndex(1f) // Ensure it's on top
                        ) {
                            DerivationView(tree, scale, offsetX, offsetY, steps, type, canvasSize)
                            if (steps == 0) {
                                LaunchedEffect(steps) {
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
                                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(text = input,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.inversePrimary,
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
                                .background(color = MaterialTheme.extendedColorScheme.accepted.colorContainer),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
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
                                .background(color = MaterialTheme.extendedColorScheme.rejected.colorContainer),
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
                        grammarViewModel.isGrammarFinished
                    ) {}
                }
            }
        }
    }
}

@Composable
fun LinearDerivation(steps: List<Step>) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(8.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text( // ε handling in derivation
            text = "S => " + steps.joinToString(" => ") {
                if (it.stateString == "${Symbols.EPSILON}") "${Symbols.EPSILON}" else it.stateString.replace("${Symbols.EPSILON}", "")
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
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
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

                    val right = if(state.appliedRule.right == "${Symbols.EPSILON}"){ // ε
                        ""
                    }else{
                        state.appliedRule.right.replace("${Symbols.EPSILON}", "")
                    }

                    // Build annotated string for displaying with different colors
                    val annotatedString = buildAnnotatedString {
                        // Unchanged part
                        append(state.previous.replace("${Symbols.EPSILON}", "").take(diffIndex))

                        // Changed part with different color
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(right)
                        }
                        // The rest of the baseString
                        append(state.previous.replace("${Symbols.EPSILON}", "").drop(diffIndex + state.appliedRule.left.length))
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
            val candidate = previous.take(i) + rule.right + previous.substring(i + left.length)
            if (candidate == current) {
                return i
            }
        }
    }
    return -1
}
