package com.sfag.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.main.ui.component.DefaultButton

@Composable
fun HomeScreen(
    navToAutomata: () -> Unit,
    navToGrammar: () -> Unit,
    navToExamples: () -> Unit,
    navToAbout: () -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_square),
                modifier = Modifier.size(312.dp),
                contentDescription = "",
            )

            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                DefaultButton(
                    onClick = { navToAutomata() },
                    text = stringResource(R.string.automata_simulator),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                DefaultButton(
                    onClick = { navToGrammar() },
                    text = stringResource(R.string.grammar_simulator),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                DefaultButton(
                    onClick = { navToExamples() },
                    text = stringResource(R.string.example_page),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                DefaultButton(
                    onClick = { navToAbout() },
                    text = stringResource(R.string.about_page),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
            }
        }
    }
}
