package com.sfag.automata.presentation.component.visualization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.tree.TreeNode
import com.sfag.shared.presentation.theme.perlamutr_white
import com.sfag.shared.presentation.theme.unable_views


@Composable
fun Machine.DerivationTree() {
    val derivationTreeElements = remember {
        mutableStateOf<List<List<TreeNode>>>(listOf())
    }

    LaunchedEffect(Unit) {
        derivationTreeElements.value = getDerivationTreeElements()
    }

    if (derivationTreeElements.value.isNotEmpty()) {
        val sumOfLeafesWeight = derivationTreeElements.value.last().sumOf { node ->
            node.weight.toInt()
        }
        val height =
            if (sumOfLeafesWeight <= 10) 350.dp else 350.dp + ((sumOfLeafesWeight - 10) * 30).dp
        LazyColumn(
            Modifier
                .height(350.dp)
                .fillMaxWidth()
                .background(perlamutr_white)
                .clip(MaterialTheme.shapes.large)
                .border(3.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
        ) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height),
                ) {
                    items(derivationTreeElements.value) { treeLevel ->
                        Column(
                            modifier = Modifier.width(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            treeLevel.forEach { treeNode ->
                                if (treeNode.stateName != null) {
                                    val backgroundColor = if (treeNode.isAccepted) {
                                        if (treeNode.isCurrent) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.background
                                        }
                                    } else {
                                        if (treeNode.isCurrent) unable_views else MaterialTheme.colorScheme.errorContainer
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .weight(treeNode.weight)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.shapes.medium
                                            )
                                            .background(backgroundColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = treeNode.stateName, fontSize = 18.sp)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(treeNode.weight))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                }
            }
        }
    }
}
