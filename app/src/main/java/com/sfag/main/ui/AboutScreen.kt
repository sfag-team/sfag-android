package com.sfag.main.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R

private enum class AboutSection {
    AUTOMATA,
    GRAMMAR,
}

@Composable
fun AboutScreen(navBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(AboutSection.AUTOMATA) }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_wide),
            contentDescription = "Logo",
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).height(60.dp),
        )

        Spacer(Modifier.height(16.dp))

        SectionCard {
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SegmentedButton(
                text = stringResource(R.string.about_automata_tab),
                active = selected == AboutSection.AUTOMATA,
            ) {
                selected = AboutSection.AUTOMATA
            }
            SegmentedButton(
                text = stringResource(R.string.about_grammar_tab),
                active = selected == AboutSection.GRAMMAR,
            ) {
                selected = AboutSection.GRAMMAR
            }
        }

        Spacer(Modifier.height(16.dp))

        when (selected) {
            AboutSection.AUTOMATA -> AutomataSection()
            AboutSection.GRAMMAR -> GrammarSection()
        }

        Spacer(Modifier.height(24.dp))

        SectionCard {
            Text(
                stringResource(R.string.authors_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            AuthorList(
                listOf(
                    "Peter Chovanec (2025)",
                    "Vadim Rohach (2025)",
                    "Jakub Tankos (2026)",
                    "Juraj Lopusek (2026)",
                    "Martin Lukacka (2026)",
                    "Samuel Strecko (2026)",
                    "Slavomir Tung Le Minh (2026)",
                )
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionCard {
            Text(
                stringResource(R.string.supervisor_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text("doc. Mgr. Daniela Chuda, PhD.", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))


        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("FEI STU Bratislava", style = MaterialTheme.typography.labelMedium)
            Text("AutoGram simulator v2.3 ©2026", style = MaterialTheme.typography.labelSmall)
        }
    }

    BackHandler { navBack() }
}

@Composable
private fun AutomataSection() {
    SectionCard {
        Text(
            text = stringResource(R.string.about_automata_features),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        FeatureList(
            listOf(
                stringResource(R.string.feature_edit_automata),
                stringResource(R.string.feature_determinism),
                stringResource(R.string.feature_formal_definition),
                stringResource(R.string.feature_visualize_derivation),
                stringResource(R.string.feature_multi_input_short),
            )
        )
    }
}

@Composable
private fun GrammarSection() {
    SectionCard {
        Text(
            text = stringResource(R.string.about_grammar_features),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        FeatureList(
            listOf(
                stringResource(R.string.feature_edit_grammar),
                stringResource(R.string.feature_grammar_type),
                stringResource(R.string.feature_formal_grammar),
                stringResource(R.string.feature_visualize_grammar),
                stringResource(R.string.feature_multi_grammar),
            )
        )
    }
}

@Composable
private fun SegmentedButton(text: String, active: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors =
            if (active) {
                ButtonDefaults.filledTonalButtonColors()
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
    ) {
        Text(text)
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun AuthorList(authors: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        authors.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(row[0], style = MaterialTheme.typography.bodyMedium)
                if (row.size > 1) {
                    Text(row[1], style = MaterialTheme.typography.bodyMedium)
                } else {
                    Spacer(Modifier.width(0.dp))
                }
            }
        }
    }
}


@Composable
private fun FeatureList(items: List<String>) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { Text(text = "- $it", style = MaterialTheme.typography.bodySmall) }
    }
}
