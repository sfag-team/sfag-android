package com.droidhen.formalautosim.presentation.navigation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.droidhen.formalautosim.R
import com.droidhen.formalautosim.presentation.activities.MainActivity
import com.droidhen.formalautosim.presentation.theme.light_blue

@Composable
fun MainScreen() {
    val recompose = remember {
        mutableIntStateOf(0)
    }
    val animation = remember {
        mutableIntStateOf(0)
    }
    var isLockedAnimation = true
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(80.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(12.dp)
                ) {
                    Text(text = "Main Screen")
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.shapes.large
                        )
                        .clip(MaterialTheme.shapes.large)
                ) {
                    key(animation.intValue){
                        if(isLockedAnimation.not()){
                            MainActivity.TestMachine.calculateTransition {
                                isLockedAnimation=true
                                recompose.intValue++
                            }
                        }
                    }
                    key(recompose.intValue) {
                        MainActivity.TestMachine.drawMachine()
                    }
                }
                Spacer(modifier = Modifier.size(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .clip(MaterialTheme.shapes.large)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.shapes.large
                        )
                        .background(light_blue),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.go_to_next),
                        contentDescription = "",
                        modifier = Modifier.clickable {
                            isLockedAnimation = false
                            animation.intValue++
                        })
                }
            }
        }
    }

}
