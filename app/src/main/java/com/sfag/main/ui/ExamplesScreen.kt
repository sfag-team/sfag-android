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
                val localizedLabel = stringResource(description.labelRes)
                val localizedDescription = stringResource(description.descRes)
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onItemClicked(example.first, localizedLabel) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = Superscripts.toDisplayString(localizedLabel),
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
            "automata/dfa_an.jff" to ExampleDescription(R.string.dfa_an_label, R.string.dfa_an_desc),
            "automata/dfa_3kplus1.jff" to
                ExampleDescription(R.string.dfa_3kplus1_label, R.string.dfa_3kplus1_desc),
            "automata/dfa_ends_01_or_10.jff" to
                ExampleDescription(
                    R.string.dfa_ends_01_or_10_label,
                    R.string.dfa_ends_01_or_10_desc,
                ),
            "automata/nfa_ends_01_or_10.jff" to
                ExampleDescription(
                    R.string.nfa_ends_01_or_10_label,
                    R.string.nfa_ends_01_or_10_desc,
                ),
            "automata/dpda_an_bn.jff" to
                ExampleDescription(R.string.dpda_an_bn_label, R.string.dpda_an_bn_desc),
            "automata/dpda_wcwR.jff" to
                ExampleDescription(R.string.dpda_wcwR_label, R.string.dpda_wcwR_desc),
            "automata/npda_wwR.jff" to
                ExampleDescription(R.string.npda_wwR_label, R.string.npda_wwR_desc),
        )

    val grammarExamples =
        listOf(
            "grammar/reg_an.jff" to ExampleDescription(R.string.reg_an_label, R.string.reg_an_desc),
            "grammar/cf_an_bn.jff" to
                ExampleDescription(R.string.cf_an_bn_label, R.string.cf_an_bn_desc),
            "grammar/cf_an_bn_norm.jff" to
                ExampleDescription(R.string.cf_an_bn_norm_label, R.string.cf_an_bn_norm_desc),
            "grammar/cs_an_bn_cn.jff" to
                ExampleDescription(R.string.cs_an_bn_cn_label, R.string.cs_an_bn_cn_desc),
            "grammar/reg_3kplus1_a.jff" to
                ExampleDescription(R.string.reg_3kplus1_a_label, R.string.reg_3kplus1_a_desc),
            "grammar/reg_ends_01_or_10.jff" to
                ExampleDescription(
                    R.string.reg_ends_01_or_10_label,
                    R.string.reg_ends_01_or_10_desc,
                ),
            "grammar/cf_wwR.jff" to
                ExampleDescription(R.string.cf_wwR_label, R.string.cf_wwR_desc),
        )
}

private data class ExampleDescription(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descRes: Int,
)
