package com.droidhen.formalautosim.presentation.navigation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import views.FASButton

@Composable
fun MainScreen(navToAutomata: () -> Unit, navToGrammar: () -> Unit, navToExamplesScreen: () -> Unit, navToAbout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FASButton(text = "navigate to Automata simulator", modifier = Modifier.width(280.dp)) {
            navToAutomata()
        }
        Spacer(modifier = Modifier.height(100.dp))
        FASButton(text = "navigate to Grammar simulator", modifier = Modifier.width(280.dp)) {
            navToGrammar()
        }
        Spacer(modifier = Modifier.height(100.dp))
        FASButton(text = "Examples", modifier = Modifier.width(200.dp)) {
            navToExamplesScreen()
        }
        Spacer(modifier = Modifier.height(100.dp))
        FASButton(text = "About", modifier = Modifier.width(200.dp)) {
            navToAbout()
        }
    }
}