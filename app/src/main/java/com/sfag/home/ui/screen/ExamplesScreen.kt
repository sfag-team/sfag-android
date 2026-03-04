package com.sfag.home.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
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
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp)
    ) {
        ExamplesSection(
            title = stringResource(R.string.automata_examples),
            items = ExampleSources.automataExamples,
            modifier = Modifier.weight(1f)
        ) { navToAutomata(it) }

        Spacer(modifier = Modifier.size(16.dp))

        ExamplesSection(
            title = stringResource(R.string.grammar_examples),
            items = ExampleSources.gramatikaExamples,
            modifier = Modifier.weight(1f)
        ) { navToGrammar(it) }
    }
    BackHandler {
        navBack()
    }
}

@Composable
private fun ExamplesSection(
    title: String,
    items: List<Pair<String, ExampleDescription>>,
    modifier: Modifier = Modifier,
    onItemClicked: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.size(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(items) { example ->
                val description = example.second
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onItemClicked(example.first) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = SuperscriptUtils.toDisplayString(description.name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Text(
                        text = description.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
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
