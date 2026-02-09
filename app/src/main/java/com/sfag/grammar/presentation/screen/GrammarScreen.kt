package com.sfag.grammar.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.grammar.domain.model.rule.GrammarRule
import com.sfag.grammar.domain.model.type.GrammarType
import com.sfag.grammar.presentation.viewmodel.GrammarViewModel
import com.sfag.shared.util.Symbols


@Composable
fun GrammarScreen(grammarViewModel: GrammarViewModel) {

    // Observe LiveData using observeAsState
    val rules by grammarViewModel.rules.observeAsState(emptyList())
    val nonterminals by grammarViewModel.nonterminals.observeAsState(emptySet())
    val terminals by grammarViewModel.terminals.observeAsState(emptySet())
    val type by grammarViewModel.grammarType.observeAsState()

    // State variables for new rule input
    var newLeft by remember { mutableStateOf(TextFieldValue("")) }
    var newRight by remember { mutableStateOf(TextFieldValue("")) }

    // Track which TextField is focused
    var focusedField by remember { mutableStateOf("") }
    var editingRule by remember { mutableStateOf<GrammarRule?>(null) }
    val finnishGrammar by grammarViewModel.isGrammarFinished.observeAsState(false)

    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "Rules P:",
            fontSize = with(LocalDensity.current) { 15.dp.toSp() },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
        )
        // LazyColumn for displaying rules and input fields
        LazyColumn(
            modifier = Modifier.weight(1f) // Makes the LazyColumn fill remaining space
        ) {
            items(rules) { rule ->
                if (editingRule == rule) {
                    var editLeft by remember(rule) { mutableStateOf(TextFieldValue(rule.left)) }
                    var editRight by remember(rule) { mutableStateOf(TextFieldValue(rule.right)) }

                    AddRule(
                        leftText = editLeft,
                        rightText = editRight,
                        focusedField = focusedField,
                        onLeftChange = { editLeft = it },
                        onRightChange = { editRight = it },
                        onFocusedFieldChange = { focusedField = it },
                        onAddRule = {
                            if (editLeft.text.isNotBlank() && editRight.text.isNotBlank()) {
                                grammarViewModel.updateRule(rule, editLeft.text, editRight.text)
                                editingRule = null
                            }
                        }
                    )
                } else {
                    DisplayRule(
                        rule = rule,
                        grammarViewModel = grammarViewModel,
                        finnishGrammar,
                        onEditClick = { editingRule = rule }
                    )
                }
            }

            // Input fields for adding a new rule
            item {
                if(!finnishGrammar){
                    AddRule(
                        leftText = newLeft,
                        rightText = newRight,
                        focusedField = focusedField,
                        onLeftChange = { newLeft = it },
                        onRightChange = { newRight = it },
                        onFocusedFieldChange = { focusedField = it },
                        onAddRule = {
                            if (newLeft.text.isNotBlank() && newRight.text.isNotBlank()) {
                                grammarViewModel.addRule(newLeft.text, newRight.text)
                                newLeft = TextFieldValue("")
                                newRight = TextFieldValue("")
                            }
                        }
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    onClick = { grammarViewModel.toggleGrammarFinished() }) {
                    Text(if (finnishGrammar) "Edit Grammar" else "Done")
                }
            }

        }
        HorizontalDivider(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            GrammarInfo(nonterminals, terminals, type)
        }
    }
}

@Composable
fun GrammarInfo(
    nonterminals: Set<Char>,
    terminals: Set<Char>,
    type: GrammarType?
) {
    val density = LocalDensity.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        item{
            Text(
                text = "G = (N, T, P, S)",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = with(density) { 25.dp.toSp() },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        item {
            Text(
                text = "Start symbol",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = with(density) { 15.dp.toSp() },
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )
        }
        item {
            Text(
                text = "S",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = with(density) { 25.dp.toSp() },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        item {
            Text(
                text = "Non-terminals",
                fontSize = with(density) { 15.dp.toSp() },
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )
        }
        item {
            Text(
                text = if (nonterminals.isNotEmpty()) {
                    "N = ${nonterminals.joinToString(", ", "{", "}")}"
                } else "N = {}",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = with(density) { 25.dp.toSp() },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        item {
            Text(
                text = "Terminals",
                fontSize = with(density) { 15.dp.toSp() },
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )
        }
        item {
            Text(
                text = if (terminals.isNotEmpty()) {
                    "T = ${terminals.joinToString(", ", "{", "}")}"
                } else "T = {}",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = with(density) { 25.dp.toSp() },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        item {
            Text(
                text = "Type",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = with(density) { 15.dp.toSp() },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        item {
            Text(
                text = type.toString(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = with(density) { 25.dp.toSp() },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun DisplayRule(rule: GrammarRule, grammarViewModel: GrammarViewModel, finnishGrammar: Boolean, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = rule.left,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("->", fontSize = 30.sp, modifier = Modifier.align(Alignment.CenterVertically))
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedTextField(
            value = rule.right,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f)
        )
        if(!finnishGrammar){
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { onEditClick() }) {
                Icon(Icons.Default.Create, contentDescription = "Edit Rule")
            }
            IconButton(onClick = { grammarViewModel.removeRule(rule) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Rule")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
fun AddRule(
    leftText: TextFieldValue,
    rightText: TextFieldValue,
    focusedField: String,
    onLeftChange: (TextFieldValue) -> Unit,
    onRightChange: (TextFieldValue) -> Unit,
    onFocusedFieldChange: (String) -> Unit,
    onAddRule: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = leftText,
            onValueChange = { onLeftChange(it) },
            placeholder = { Text("Left Side") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) onFocusedFieldChange("left")
                }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("->", fontSize = 30.sp, modifier = Modifier.align(Alignment.CenterVertically))
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedTextField(
            value = rightText,
            onValueChange = { onRightChange(it) },
            placeholder = { Text("Right Side") },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) onFocusedFieldChange("right")
                }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxHeight().width(30.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    when (focusedField) {
                        "left" -> onLeftChange(
                            TextFieldValue(leftText.text + "|", TextRange(leftText.text.length + 1))
                        )
                        "right" -> onRightChange(
                            TextFieldValue(rightText.text + "|", TextRange(rightText.text.length + 1))
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(2.dp)
            ) { Text("|", fontSize = 15.sp) }

            FilledTonalButton(
                onClick = {
                    when (focusedField) {
                        "left" -> onLeftChange(
                            TextFieldValue(leftText.text + Symbols.EPSILON, TextRange(leftText.text.length + 1))
                        )
                        "right" -> onRightChange(
                            TextFieldValue(rightText.text + Symbols.EPSILON, TextRange(rightText.text.length + 1))
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(2.dp)
            ) { Text(Symbols.EPSILON, fontSize = 15.sp) }
        }

        Spacer(modifier = Modifier.width(4.dp))
        FilledIconButton(
            onClick = {
                val validChars = leftText.text.all { it.isLetterOrDigit() || it == '|' || it == Symbols.EPSILON_CHAR } &&
                        rightText.text.all { it.isLetterOrDigit() || it == '|' || it == Symbols.EPSILON_CHAR }
                if (!validChars) {
                    Toast.makeText(context, "Only letters, digits, ${Symbols.EPSILON} or | are allowed", Toast.LENGTH_SHORT).show()
                } else if (!leftText.text.any { it.isUpperCase() }) {
                    Toast.makeText(context, "Non-terminal symbol missing on the left side", Toast.LENGTH_SHORT).show()
                } else {
                    onAddRule()
                    focusManager.clearFocus()
                }

            },
            shape = RoundedCornerShape(2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Rule")
        }
    }
}
