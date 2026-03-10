package com.sfag.automata.ui.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sfag.R
import com.sfag.automata.ui.common.BAR_HEIGHT
import com.sfag.automata.ui.common.CELL_SIZE
import com.sfag.automata.ui.machine.MachineEditMode

@Composable
internal fun Toolbar(
    activeTool: MachineEditMode,
    onSelectTool: (MachineEditMode) -> Unit,
) {
    val cellPadding = (BAR_HEIGHT - CELL_SIZE) / 2
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .padding(horizontal = cellPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = CenterVertically,
    ) {
        ToolIcon(R.drawable.select, R.string.select_tool, MachineEditMode.SELECT, activeTool, onSelectTool)
        ToolIcon(R.drawable.move, R.string.move_tool, MachineEditMode.MOVE, activeTool, onSelectTool)
        ToolIcon(
            R.drawable.add_states,
            R.string.add_state,
            MachineEditMode.ADD_STATES,
            activeTool,
            onSelectTool,
        )
        ToolIcon(
            R.drawable.add_transitions,
            R.string.add_transition,
            MachineEditMode.ADD_TRANSITIONS,
            activeTool,
            onSelectTool,
        )
        ToolIcon(R.drawable.delete, R.string.remove_item, MachineEditMode.REMOVE, activeTool, onSelectTool)
    }
}

@Composable
private fun ToolIcon(
    icon: Int,
    label: Int,
    tool: MachineEditMode,
    activeTool: MachineEditMode,
    onSelectTool: (MachineEditMode) -> Unit,
) {
    val isSelected = activeTool == tool
    Icon(
        painter = painterResource(id = icon),
        contentDescription = stringResource(label),
        modifier =
            Modifier
                .size(CELL_SIZE)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ).clickable { onSelectTool(tool) }
                .padding((BAR_HEIGHT - CELL_SIZE) / 2),
        tint =
            if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
    )
}
