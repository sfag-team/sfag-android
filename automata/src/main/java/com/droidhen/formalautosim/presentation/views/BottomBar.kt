package com.droidhen.formalautosim.presentation.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidhen.automata.R
import com.droidhen.formalautosim.presentation.navigation.AutomataDestinations

@Composable
fun BottomBar(navController: NavController) {
    val weights = remember {
        mutableStateOf(listOf(3f, 2f, 2f))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(start = 16.dp, end = 16.dp)
            ) {
                Box(modifier = Modifier.weight(weights.value[0])) {
                    Circle(weight = weights.value[0])
                    Image(painter = painterResource(id = R.drawable.home),
                        contentDescription = "home screen icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 7.dp)
                            .clickable {
                                if (weights.value[0] == 2f) {
                                    weights.value = listOf(3f,2f,2f)
                                    navController.navigate(AutomataDestinations.AUTOMATA.route)
                                }
                            })
                }

                Spacer(modifier = Modifier.weight(0.5f))
                Box(modifier = Modifier.weight(weights.value[1])) {
                    Circle(weight = weights.value[1])
                    Image(painter = painterResource(id = R.drawable.community),
                        contentDescription = "community screen icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 7.dp)
                            .clickable {
                                if (weights.value[1] == 2f) {
                                    weights.value = listOf(2f,3f,2f)
                                    navController.navigate(AutomataDestinations.COMMUNITY.route)
                                }
                            })
                }

                Spacer(modifier = Modifier.weight(0.5f))
                Box(modifier = Modifier.weight(weights.value[2])) {
                    Circle(weight = weights.value[2])
                    Image(painter = painterResource(id = R.drawable.settings),
                        contentDescription = "settings screen icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 7.dp)
                            .clickable {
                                if (weights.value[2] == 2f) {
                                    weights.value = listOf(2f,2f,3f)
                                    navController.navigate(AutomataDestinations.USER_PROFILE.route)
                                }
                            })
                }
            }

        Spacer(modifier = Modifier
            .height(5.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondary))
    }
}

@Composable
fun Circle(weight:Float){
    val circleColor = MaterialTheme.colorScheme.surface
    if (weight != 2f) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            drawCircle(
                color = circleColor,
                radius = size.minDimension / 2
            )
        }
    }
}