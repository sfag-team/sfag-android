package com.sfag.automata.presentation.component.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R


@Composable
fun DropDownSelector(
    items: List<Any>,
    label: Any = "Chose an item",
    defaultSelectedIndex: Int = 0,
    onItemSelected: (Any) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(label) }


    LaunchedEffect(Unit) {
        if (items.isNotEmpty() && defaultSelectedIndex in items.indices) {
            selectedText = items[defaultSelectedIndex]
            onItemSelected(selectedText)
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopCenter)
            .padding(start = 4.dp, end = 4.dp)
    ) {

        OutlinedButton(onClick = { expanded = true }) {
            Text(text = selectedText.toString())
        }


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(text = item.toString()) },
                    onClick = {
                        selectedText = item.toString()
                        expanded = false
                        onItemSelected(item)
                    }
                )
            }
        }
    }
}

@Composable
fun RowScope.ItemSpecificationIcon(
    icon: Int,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Spacer(modifier = Modifier.weight(1f))
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .weight(3.5f)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                3.dp,
                MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
            .clickable {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = stringResource(
                R.string.initial_state
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )
        Text(text = text)
    }
}
