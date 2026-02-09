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
import androidx.compose.ui.unit.sp
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
        DefaultButton(text = "BACK") {
            navBack()
        }
        Spacer(modifier = Modifier.size(12.dp))
        ImmutableTextField(
            text = "Automata examples",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.size(8.dp))
        ExamplesColumn(ExampleSources.automataExamples){
            navToAutomata(it)
        }
        Spacer(modifier = Modifier.size(24.dp))
        ImmutableTextField(
            text = "Grammar examples",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.size(8.dp))
        ExamplesColumn(ExampleSources.gramatikaExamples){
            navToGrammar(it)
        }
    }
    BackHandler {
        navBack()
    }
}


@Composable
private fun ExamplesColumn(items: List<Pair<String, ExampleDescription>>, onItemClicked: (String) -> Unit){
    LazyColumn(
        modifier = Modifier
            .height(240.dp)
            .fillMaxWidth()
            .border(3.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraLarge)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items) { example ->
            val description = example.second
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(8.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onItemClicked(example.first) },
                horizontalAlignment = Alignment.CenterHorizontally,

                ) {
                Text(text = SuperscriptUtils.toDisplayString(description.name), fontSize = 21.sp, modifier = Modifier.padding(start = 6.dp, end = 6.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = description.description, fontSize = 21.sp, modifier = Modifier.padding(start = 6.dp, end = 6.dp))
            }
        }
    }
}

internal object ExampleSources {
    val automataExamples = listOf(
        "automata/a-n.jff" to ExampleDescription("a^n", "deterministic finite automaton"),
        "automata/3k-1.jff" to ExampleDescription("3k +1", "deterministic finite automaton"),
        "automata/ends-dfa.jff" to ExampleDescription("Ends 01 or 10", "deterministic finite automaton"),
        "automata/ends-nfa.jff" to ExampleDescription("Ends 01 or 10", "nondeterministic finite automaton"),
        "automata/an-bn-pda.jff" to ExampleDescription("a^n b^n", "deterministic pushdown automaton"),
        "automata/wcw-r.jff" to ExampleDescription("wcwR", "deterministic pushdown automaton"),
        "automata/ww-r.jff" to ExampleDescription("wwR", "nondeterministic pushdown automaton")
    )

    val gramatikaExamples = listOf(
        "grammar/g-reg.jff" to ExampleDescription(
            "a^n",
            "Regular Grammar"
        ),
        "grammar/g-cf.jff" to ExampleDescription(
            "a^n b^n",
            "Context-Free Grammar"
        ),
        "grammar/ab_norm.jff" to ExampleDescription(
            "Normalized a^n b^n",
            "Context-Free Grammar"
        ),
        "grammar/g-cs.jff" to ExampleDescription(
            "a^n b^n c^n",
            "Context-Sensitive Grammar"
        ),
        "grammar/gram-3kplus1-a.jff" to ExampleDescription(
            "3k+1 a",
            "Regular Grammar"
        ),
        "grammar/gram-01.jff" to ExampleDescription(
            "Ends 01 or 10",
            "Regular grammar"
        ),
        "grammar/gram-wwR.jff" to ExampleDescription(
            "wwR",
            "Context-Free Grammar"
        ),
    )
}

data class ExampleDescription(val name: String, val description: String)
