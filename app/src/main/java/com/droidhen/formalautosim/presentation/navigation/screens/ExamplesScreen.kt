package com.droidhen.formalautosim.presentation.navigation.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType.Companion.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import views.FASButton
import views.FASImmutableTextField

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
        FASButton(text = "BACK") {
            navBack()
        }
        Spacer(modifier = Modifier.size(12.dp))
        FASImmutableTextField(
            text = "Automata examples",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.size(8.dp))
        ExamplesColumn(ExampleSources.automataExamples){
            navToAutomata(it)
        }
        Spacer(modifier = Modifier.size(24.dp))
        FASImmutableTextField(
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
private fun ExamplesColumn(items: List<Pair<String, ExampleDescription>>,onItemClicked: (String) -> Unit){
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
                Text(text = description.name, fontSize = 21.sp, modifier = Modifier.padding(start = 6.dp, end = 6.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = description.description, fontSize = 21.sp, modifier = Modifier.padding(start = 6.dp, end = 6.dp))
            }
        }
    }
}

object ExampleSources {
    val automataExamples = listOf(
        "automata/pda_determ.jff" to ExampleDescription(
            "PDA deterministic",
            "Push-down automata, tests aⁿbⁿ words"
        ),
        "automata/pda_no_determ.jff" to ExampleDescription(
            "PDA no deterministic",
            "Push-down automata, no deterministic tests"
        ),
    )
    val gramatikaExamples = listOf(
        "grammar/g-reg.jff" to ExampleDescription(
            "aⁿ",
            "Regular Grammar"
        ),
        "grammar/g-cf.jff" to ExampleDescription(
            "aⁿbⁿ",
            "Context-Free Grammar"
        ),
        "grammar/ab_norm.jff" to ExampleDescription(
            "Normalized aⁿbⁿ",
            "Context-Free Grammar"
        ),
        "grammar/g-cs.jff" to ExampleDescription(
            "aⁿbⁿcⁿ",
            "Context-Sensitive Grammar"
        ),
        "grammar/ggram-3kplus1-a.jff" to ExampleDescription(
            "3k+1 a",
            "Regular Grammar"
        ),
        "grammar/gram-01.jff" to ExampleDescription(
            "String of 0 and 1",
            "Regular grammar"
        ),
        "grammar/gram-wwR.jff" to ExampleDescription(
            "wwR",
            "Context-Free Grammar"
        ),
    )
}

data class ExampleDescription(val name: String, val description: String)