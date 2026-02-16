package com.sfag.home.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sfag.shared.presentation.component.DefaultButton
import com.sfag.shared.presentation.component.ImmutableTextField
import com.sfag.shared.util.SuperscriptUtils

@Composable
fun ExamplesScreen(
    navBack: () -> Unit,
    navToAutomata: (machineUri: String) -> Unit,
    navToGrammar: (grammarUri: String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        DefaultButton(text = "Back") {
            navBack()
        }
        Spacer(modifier = Modifier.size(16.dp))
        ImmutableTextField(
            text = "Automata examples",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        ExamplesColumn(ExampleSources.automataExamples, modifier = Modifier.weight(1f)){
            navToAutomata(it)
        }
        Spacer(modifier = Modifier.size(16.dp))
        ImmutableTextField(
            text = "Grammar examples",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.size(8.dp))
        ExamplesColumn(ExampleSources.gramatikaExamples, modifier = Modifier.weight(1f)){
            navToGrammar(it)
        }
    }
    BackHandler {
        navBack()
    }
}


@Composable
private fun ExamplesColumn(items: List<Pair<String, ExampleDescription>>, modifier: Modifier = Modifier, onItemClicked: (String) -> Unit){
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items) { example ->
            val description = example.second
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(8.dp)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.shapes.small
                    )
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onItemClicked(example.first) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
                ) {
                Text(
                    text = SuperscriptUtils.toDisplayString(description.name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = description.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private object ExampleSources {
    val automataExamples = listOf(
        "automata/a-n.jff" to ExampleDescription("a^n", "Deterministic finite automaton"),
        "automata/3k-1.jff" to ExampleDescription("3k +1", "Deterministic finite automaton"),
        "automata/ends-dfa.jff" to ExampleDescription("Ends 01 or 10", "Deterministic finite automaton"),
        "automata/ends-nfa.jff" to ExampleDescription("Ends 01 or 10", "Non-deterministic finite automaton"),
        "automata/an-bn-pda.jff" to ExampleDescription("a^n b^n", "Deterministic pushdown automaton"),
        "automata/wcw-r.jff" to ExampleDescription("wcwR", "Deterministic pushdown automaton"),
        "automata/ww-r.jff" to ExampleDescription("wwR", "Non-deterministic pushdown automaton")
    )

    val gramatikaExamples = listOf(
        "grammar/g-reg.jff" to ExampleDescription("a^n", "Regular grammar"),
        "grammar/g-cf.jff" to ExampleDescription("a^n b^n", "Context-free grammar"),
        "grammar/ab_norm.jff" to ExampleDescription("Normalized a^n b^n", "Context-free grammar"),
        "grammar/g-cs.jff" to ExampleDescription("a^n b^n c^n", "Context-sensitive grammar"),
        "grammar/gram-3kplus1-a.jff" to ExampleDescription("3k+1 a", "Regular grammar"),
        "grammar/gram-01.jff" to ExampleDescription("Ends 01 or 10", "Regular grammar"),
        "grammar/gram-wwR.jff" to ExampleDescription("wwR", "Context-free grammar"),
    )
}

private data class ExampleDescription(val name: String, val description: String)
