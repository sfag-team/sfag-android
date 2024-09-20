package com.droidhen.formalautosim.presentation.navigation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.unit.times
import com.droidhen.formalautosim.presentation.activities.MainActivity
import com.droidhen.formalautosim.utils.extensions.drawArrow

@Composable
fun MainScreen() {
    var recompose = remember {
        mutableIntStateOf(0)
    }
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
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.large
                        )
                        .clip(MaterialTheme.shapes.large)
                ) {
                    key (recompose.intValue){
                        MainActivity.TestMachine.drawMachine()
                        MainActivity.TestMachine.simulateTransition{
                            recompose.intValue ++
                        }
                    }
                }
            }
        }
    }

}
