package com.sfag.grammar.ui.input

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sfag.R
import com.sfag.grammar.domain.grammar.DerivationStep
import com.sfag.grammar.domain.grammar.GrammarType
import com.sfag.grammar.domain.grammar.ParseResult
import com.sfag.grammar.domain.grammar.findReplacementIndex
import com.sfag.grammar.domain.grammar.parse
import com.sfag.grammar.ui.GrammarViewModel
import com.sfag.grammar.ui.common.GrammarRuleView
import com.sfag.grammar.ui.tree.TreeView
import com.sfag.grammar.ui.tree.buildTree
import com.sfag.grammar.ui.tree.focusNodeAnimated
import com.sfag.grammar.ui.tree.layoutPrettyTreeRestricted
import com.sfag.grammar.ui.tree.layoutPrettyTreeUnrestricted
import com.sfag.main.config.Symbols
import com.sfag.main.ui.component.ChevronLeft
import com.sfag.main.ui.component.ChevronRight
import com.sfag.main.ui.component.KeyboardDoubleArrowRight
import com.sfag.main.ui.component.NetworkNode
import com.sfag.main.ui.theme.extendedColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SingleInputView(
    grammarViewModel: GrammarViewModel,
    initialInput: String?,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf(initialInput ?: "") }
    var testedInput by remember { mutableStateOf(initialInput) }
    val rules = grammarViewModel.getIndividualRules()
    val terminals = grammarViewModel.terminals
    val grammarType = grammarViewModel.grammarType
    var parseResult by remember { mutableStateOf<ParseResult?>(null) }
    var isParsed by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }
    var isTableShown by remember { mutableStateOf(false) }
    var isLinearShown by remember { mutableStateOf(false) }
    var isTreeShown by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Parse preInput on background thread when screen opens
    LaunchedEffect(Unit) {
        if (initialInput != null) {
            isParsing = true
            parseResult =
                withContext(Dispatchers.Default) {
                    parse(initialInput, rules, terminals, grammarType)
                }
            isParsed = true
            isParsing = false
            isTableShown = true
        }
    }

    val scale = remember { mutableFloatStateOf(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val canvasSize = remember { mutableStateOf(Size.Zero) }

    val steps = (parseResult as? ParseResult.Success)?.steps

    // Defer tree layout until the user opens the tree view
    val treeData =
        remember(parseResult, isTreeShown) {
            if (!isTreeShown) {
                return@remember null
            }
            steps?.let {
                val root = buildTree(it)
                val pos =
                    if (
                        grammarType == GrammarType.UNRESTRICTED ||
                            grammarType == GrammarType.CONTEXT_SENSITIVE
                    ) {
                        layoutPrettyTreeUnrestricted(root)
                    } else {
                        layoutPrettyTreeRestricted(root)
                    }
                root to pos
            }
        }
    val tree = treeData?.first
    val treePositions = treeData?.second

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.test_an_input),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalDivider(
            modifier = Modifier.height(4.dp).fillMaxWidth().padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    isParsed = false
                    inputText = it
                },
                placeholder = { Text(stringResource(R.string.terminal_placeholder)) },
                modifier = Modifier.weight(1f).padding(top = 4.dp),
                singleLine = true,
                colors =
                    TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    testedInput = inputText
                    isTreeShown = false
                    isTableShown = false
                    isLinearShown = false
                    focusManager.clearFocus()
                    coroutineScope.launch {
                        isParsing = true
                        parseResult =
                            withContext(Dispatchers.Default) {
                                parse(inputText, rules, terminals, grammarType)
                            }
                        isParsed = true
                        isParsing = false
                    }
                }
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.check_input),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (isParsing) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        if (isParsed && !isParsing) {
            if (isTreeShown && steps != null && tree != null && treePositions != null) {
                var treeStep by remember { mutableIntStateOf(0) }
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(top = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceDim)
                            .zIndex(1f)
                ) {
                    TreeView(
                        tree,
                        treePositions,
                        scale,
                        offsetX,
                        offsetY,
                        treeStep,
                        grammarType,
                        canvasSize,
                    )
                    if (treeStep == 0) {
                        LaunchedEffect(treeStep) {
                            focusNodeAnimated(
                                tree,
                                treePositions,
                                treeStep,
                                offsetX,
                                offsetY,
                                scale.floatValue,
                                canvasSize.value.width,
                                canvasSize.value.height,
                            )
                        }
                    }
                    if (treeStep == 1 + steps.size) {
                        Box(
                            modifier = Modifier.fillMaxSize().zIndex(2f).padding(bottom = 64.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.derivation_completed),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = inputText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { isTreeShown = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_tree),
                        )
                    }
                    IconButton(
                        onClick = { treeStep = 0 },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset_simulation),
                        )
                    }
                    IconButton(
                        onClick = {
                            if (treeStep < steps.size) {
                                coroutineScope.launch {
                                    treeStep++
                                    focusNodeAnimated(
                                        tree,
                                        treePositions,
                                        treeStep,
                                        offsetX,
                                        offsetY,
                                        scale.floatValue,
                                        canvasSize.value.width,
                                        canvasSize.value.height,
                                    )
                                }
                            } else if (treeStep == steps.size) {
                                treeStep++
                            }
                        },
                        modifier =
                            Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 4.dp),
                    ) {
                        Icon(ChevronRight, contentDescription = stringResource(R.string.next_step))
                    }
                    IconButton(
                        onClick = {
                            if (treeStep > 0) {
                                coroutineScope.launch {
                                    treeStep--
                                    focusNodeAnimated(
                                        tree,
                                        treePositions,
                                        treeStep,
                                        offsetX,
                                        offsetY,
                                        scale.floatValue,
                                        canvasSize.value.width,
                                        canvasSize.value.height,
                                    )
                                }
                            }
                        },
                        modifier =
                            Modifier.align(Alignment.BottomStart)
                                .padding(start = 30.dp, bottom = 4.dp),
                    ) {
                        Icon(
                            ChevronLeft,
                            contentDescription = stringResource(R.string.previous_step),
                        )
                    }
                }
            }
            // Result banner
            val acceptedText = stringResource(R.string.input_accepted)
            val notAcceptedText = stringResource(R.string.input_not_accepted)
            val inconclusiveText = stringResource(R.string.input_inconclusive)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp)) {
                val inputSpanStyle =
                    MaterialTheme.typography.titleLarge
                        .toSpanStyle()
                        .copy(fontStyle = FontStyle.Italic)
                when (parseResult) {
                    is ParseResult.Success -> {
                        Text(
                            text =
                                buildAnnotatedString {
                                    withStyle(style = inputSpanStyle) { append("\"$testedInput\"") }
                                    append(acceptedText)
                                },
                            modifier =
                                Modifier.weight(1f)
                                    .background(
                                        color =
                                            MaterialTheme.extendedColorScheme.accepted
                                                .colorContainer
                                    ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    is ParseResult.Rejected -> {
                        Text(
                            text =
                                buildAnnotatedString {
                                    withStyle(style = inputSpanStyle) { append("\"$testedInput\"") }
                                    append(notAcceptedText)
                                },
                            modifier =
                                Modifier.weight(1f)
                                    .background(
                                        color =
                                            MaterialTheme.extendedColorScheme.rejected
                                                .colorContainer
                                    ),
                            textAlign = TextAlign.Center,
                        )
                    }

                    is ParseResult.Inconclusive -> {
                        Text(
                            text =
                                buildAnnotatedString {
                                    withStyle(style = inputSpanStyle) { append("\"$testedInput\"") }
                                    append(inconclusiveText)
                                },
                            modifier =
                                Modifier.weight(1f)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    null -> {}
                }
            }
            if (steps != null) {
                // Visual options
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    IconButton(
                        onClick = {
                            isTableShown = false
                            isLinearShown = true
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            stringResource(R.string.linear_derivation),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = {
                            isLinearShown = false
                            isTableShown = true
                        }
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            stringResource(R.string.table_derivation),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { isTreeShown = true }) {
                        Icon(
                            NetworkNode,
                            stringResource(R.string.tree_derivation),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (isTableShown) {
                    StateTable(steps, Modifier.padding(horizontal = 16.dp))
                }
                if (isLinearShown) {
                    LinearDerivation(steps, Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        if (!isParsed || parseResult !is ParseResult.Success) {
            Text(
                text = stringResource(R.string.grammar_rule),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
            )
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(rules) { rule ->
                    GrammarRuleView(
                        grammarRule = rule,
                        grammarViewModel = grammarViewModel,
                        grammarViewModel.isGrammarFinished,
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun LinearDerivation(steps: List<DerivationStep>, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.shapes.medium,
                )
                .padding(8.dp)
                .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // ε handling in derivation
            text =
                "S => " +
                    steps.joinToString(" => ") {
                        if (it.derived == Symbols.EPSILON) {
                            Symbols.EPSILON
                        } else {
                            it.derived.replace(Symbols.EPSILON, "")
                        }
                    },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StateTable(steps: List<DerivationStep>, modifier: Modifier = Modifier) {
    var currentStep by remember { mutableIntStateOf(1) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.shapes.medium,
                )
                .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { if (currentStep > 1) currentStep-- }) {
                Icon(ChevronLeft, contentDescription = stringResource(R.string.previous_step))
            }
            IconButton(onClick = { if (currentStep < steps.size) currentStep++ }) {
                Icon(ChevronRight, contentDescription = stringResource(R.string.next_step))
            }
            IconButton(onClick = { currentStep = steps.size }) {
                Icon(
                    KeyboardDoubleArrowRight,
                    contentDescription = stringResource(R.string.jump_to_end),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.applied_rule),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.derivation_string),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
        }
        HorizontalDivider()

        LazyColumn {
            items(steps.take(currentStep)) { derivation ->
                val diffIndex =
                    findReplacementIndex(
                        derivation.appliedRule,
                        derivation.previous,
                        derivation.derived,
                    )

                val right =
                    if (derivation.appliedRule.right == Symbols.EPSILON) {
                        ""
                    } else {
                        derivation.appliedRule.right.replace(Symbols.EPSILON, "")
                    }

                val annotatedString = buildAnnotatedString {
                    if (diffIndex >= 0) {
                        append(derivation.previous.replace(Symbols.EPSILON, "").take(diffIndex))
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(right)
                    }
                    if (diffIndex >= 0) {
                        append(
                            derivation.previous
                                .replace(Symbols.EPSILON, "")
                                .drop(diffIndex + derivation.appliedRule.left.length)
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = derivation.appliedRule.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = annotatedString,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
