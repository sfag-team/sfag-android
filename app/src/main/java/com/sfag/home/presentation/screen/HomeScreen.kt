package com.sfag.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.shared.presentation.component.DefaultButton

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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(8.dp))
        Image(
            painter = painterResource(id = R.drawable.splash),
            modifier = Modifier.size(320.dp),
            contentDescription = ""
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            DefaultButton(text = "Automata simulator", modifier = Modifier.fillMaxWidth(), height = 64) {
                navToAutomata()
            }
            DefaultButton(text = "Grammar simulator", modifier = Modifier.fillMaxWidth(), height = 64) {
                navToGrammar()
            }
            DefaultButton(text = "Examples", modifier = Modifier.fillMaxWidth(), height = 64) {
                navToExamplesScreen()
            }
            DefaultButton(text = "About", modifier = Modifier.fillMaxWidth(), height = 64) {
                navToAbout()
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
    }
}
