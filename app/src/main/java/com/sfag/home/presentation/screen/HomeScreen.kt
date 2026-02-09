package com.sfag.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(32.dp))
        Image(
            painter = painterResource(id = R.drawable.splash),
            modifier = Modifier.size(300.dp),
            contentDescription = ""
        )

        Column(
            modifier = Modifier
                .height(400.dp)
                .width(280.dp)
                .border(3.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            DefaultButton(text = "navigate to Automata simulator", modifier = Modifier.width(250.dp), height = 60) {
                navToAutomata()
            }
            Spacer(modifier = Modifier.height(40.dp))
            DefaultButton(text = "navigate to Grammar simulator", modifier = Modifier.width(250.dp), height = 60) {
                navToGrammar()
            }
            Spacer(modifier = Modifier.height(40.dp))
            DefaultButton(text = "Examples", modifier = Modifier.width(250.dp)) {
                navToExamplesScreen()
            }
            Spacer(modifier = Modifier.height(40.dp))
            DefaultButton(text = "About", modifier = Modifier.width(250.dp)) {
                navToAbout()
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
