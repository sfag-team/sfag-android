package com.sfag.main.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.sfag.R
import com.sfag.main.config.Superscripts

@Composable
fun ExamplesScreen(
    navBack: () -> Unit,
    navToAutomata: (exampleUri: String, name: String) -> Unit,
    navToGrammar: (exampleUri: String, name: String) -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ExamplesSection(
            title = stringResource(R.string.automata_example),
            items = ExampleSources.automataExamples,
            modifier = Modifier.weight(1f),
        ) { uri, name ->
            navToAutomata(uri, name)
        }

        ExamplesSection(
            title = stringResource(R.string.grammar_example),
            items = ExampleSources.grammarExamples,
            modifier = Modifier.weight(1f),
        ) { uri, name ->
            navToGrammar(uri, name)
        }
    }
    BackHandler { navBack() }
}

@Composable
private fun ExamplesSection(
    title: String,
    items: List<Pair<String, ExampleDescription>>,
    modifier: Modifier = Modifier,
    onItemClicked: (uri: String, name: String) -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(items) { example ->
                val description = example.second
                val localizedName = stringResource(description.nameRes)
                val localizedDescription = stringResource(description.descriptionRes)
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onItemClicked(example.first, localizedName) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = Superscripts.toDisplayString(localizedName),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        text = localizedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

private object ExampleSources {
    val automataExamples =
        listOf(
            "automata/a-n.jff" to
                ExampleDescription(R.string.example_name_an, R.string.example_desc_dfa),
            "automata/3k-1.jff" to
                ExampleDescription(R.string.example_name_3k_plus_1, R.string.example_desc_dfa),
            "automata/ends-dfa.jff" to
                ExampleDescription(
                    R.string.example_name_ends_01_or_10,
                    R.string.example_desc_dfa,
                ),
            "automata/ends-nfa.jff" to
                ExampleDescription(
                    R.string.example_name_ends_01_or_10,
                    R.string.example_desc_ndfa,
                ),
            "automata/an-bn-pda.jff" to
                ExampleDescription(R.string.example_name_an_bn, R.string.example_desc_dpda),
            "automata/wcw-r.jff" to
                ExampleDescription(R.string.example_name_wcw_r, R.string.example_desc_dpda),
            "automata/ww-r.jff" to
                ExampleDescription(R.string.example_name_ww_r, R.string.example_desc_ndpda),
        )

    val grammarExamples =
        listOf(
            "grammar/g-reg.jff" to
                ExampleDescription(R.string.example_name_an, R.string.example_desc_regular_grammar),
            "grammar/g-cf.jff" to
                ExampleDescription(R.string.example_name_an_bn, R.string.example_desc_context_free_grammar),
            "grammar/ab_norm.jff" to
                ExampleDescription(
                    R.string.example_name_normalized_an_bn,
                    R.string.example_desc_context_free_grammar,
                ),
            "grammar/g-cs.jff" to
                ExampleDescription(
                    R.string.example_name_an_bn_cn,
                    R.string.example_desc_context_sensitive_grammar,
                ),
            "grammar/gram-3kplus1-a.jff" to
                ExampleDescription(
                    R.string.example_name_3k_plus_1_a,
                    R.string.example_desc_regular_grammar,
                ),
            "grammar/gram-01.jff" to
                ExampleDescription(
                    R.string.example_name_ends_01_or_10,
                    R.string.example_desc_regular_grammar,
                ),
            "grammar/gram-wwR.jff" to
                ExampleDescription(
                    R.string.example_name_ww_r,
                    R.string.example_desc_context_free_grammar,
                ),
        )
}

private data class ExampleDescription(
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
)
