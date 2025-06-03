package com.droidhen.formalautosim.presentation.navigation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.droidhen.formalautosim.R
import views.FASButton

@Composable
fun MainScreen(
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
                .height(380.dp)
                .width(280.dp)
                .border(3.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            FASButton(text = "navigate to Automata simulator", modifier = Modifier.width(250.dp), height = 60) {
                navToAutomata()
            }
            Spacer(modifier = Modifier.height(40.dp))
            FASButton(text = "navigate to Grammar simulator", modifier = Modifier.width(250.dp)) {
                navToGrammar()
            }
            Spacer(modifier = Modifier.height(40.dp))
            FASButton(text = "Examples", modifier = Modifier.width(250.dp)) {
                navToExamplesScreen()
            }
            Spacer(modifier = Modifier.height(40.dp))
            FASButton(text = "About", modifier = Modifier.width(250.dp)) {
                navToAbout()
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}