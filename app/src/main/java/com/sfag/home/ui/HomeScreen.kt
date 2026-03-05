package com.sfag.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.sfag.R
import com.sfag.shared.ui.component.DefaultButton

@Composable
fun HomeScreen(
    navToAutomata: () -> Unit,
    navToGrammar: () -> Unit,
    navToExamplesScreen: () -> Unit,
    navToAbout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_square),
            modifier = Modifier.size(312.dp),
            contentDescription = ""
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DefaultButton(text = stringResource(R.string.automata_simulator), modifier = Modifier.fillMaxWidth(), height = 56) {
                navToAutomata()
            }
            DefaultButton(text = stringResource(R.string.grammar_simulator), modifier = Modifier.fillMaxWidth(), height = 56) {
                navToGrammar()
            }
            DefaultButton(text = stringResource(R.string.examples), modifier = Modifier.fillMaxWidth(), height = 56) {
                navToExamplesScreen()
            }
            DefaultButton(text = stringResource(R.string.about), modifier = Modifier.fillMaxWidth(), height = 56) {
                navToAbout()
            }
        }
    }
}
