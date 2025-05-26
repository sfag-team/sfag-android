package com.example.gramatika

import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gramatika.ui.theme.light_blue


@Composable
fun BulkTestScreen(navController: NavController, grammarViewModel: Grammar, inputsViewModel: Inputs) {
    val inputs by inputsViewModel.inputs.observeAsState(emptyList())
    val rules = grammarViewModel.getIndividualRules()
    val terminals = grammarViewModel.terminals.value ?: emptySet()
    val grammarType = grammarViewModel.grammarType.value ?: GrammarType.UNRESTRICTED

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            text = "Test multiple inputs",
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
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(inputs.indices.toList()) { index ->
                if(inputs.size > 5 && index != inputs.lastIndex && inputs[index] == ""){
                    inputsViewModel.removeRowAt(index)
                }else{
                    TableRow(
                        text = inputs[index],
                        onTextChange = { newText ->
                            inputsViewModel.updateRowText(index, newText)
                            if (index == inputs.lastIndex && newText.isNotBlank()) {
                                inputsViewModel.addRow()
                            }
                        },
                        rules = rules,
                        terminals = terminals,
                        type = grammarType,
                        navToDerivation = { inputText ->
                            navController.navigate("testScreen?input=${Uri.encode(inputText)}")
                        }
                    )}
            }
        }
    }
}

@Composable
fun TableRow(text: String, onTextChange: (String) -> Unit, rules: List<GrammarRule>, terminals: Set<Char>, type :GrammarType, navToDerivation: (String) -> Unit) {
    val isValid: Boolean = parse(text, rules, terminals, type) != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = light_blue,
                focusedContainerColor = light_blue
            )
            )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = if (isValid) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (isValid) "Valid" else "Invalid",
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        if(isValid){
            IconButton(onClick = { navToDerivation(text) }){
                Icon(Icons.Default.PlayArrow, contentDescription = "See derivation")
            }
        }
    }
}
