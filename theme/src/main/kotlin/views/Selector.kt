package views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DropdownSelector(
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
            .padding(4.dp)
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