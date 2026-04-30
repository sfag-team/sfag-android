package com.sfag.automata.ui.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sfag.R
import com.sfag.automata.ui.machine.MachineEditMode
import com.sfag.automata.ui.machine.cellPadding
import com.sfag.automata.ui.machine.cellSize

@Composable
internal fun Toolbar(activeTool: MachineEditMode, onSelectTool: (MachineEditMode) -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(cellSize + cellPadding * 2)
                .padding(horizontal = cellPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = CenterVertically,
    ) {
        ToolIcon(
            Icons.Outlined.NearMe,
            R.string.select_tool,
            MachineEditMode.SELECT,
            activeTool,
            onSelectTool,
        )
        ToolIcon(
            Icons.Outlined.OpenWith,
            R.string.move_tool,
            MachineEditMode.MOVE,
            activeTool,
            onSelectTool,
        )
        ToolIcon(
            R.drawable.add_states,
            R.string.add_state,
            MachineEditMode.ADD_STATE,
            activeTool,
            onSelectTool,
        )
        ToolIcon(
            R.drawable.add_transitions,
            R.string.add_transition,
            MachineEditMode.ADD_TRANSITION,
            activeTool,
            onSelectTool,
        )
        ToolIcon(
            Icons.Outlined.Delete,
            R.string.remove_item,
            MachineEditMode.REMOVE,
            activeTool,
            onSelectTool,
        )
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
    ToolIconBox(tool, activeTool, onSelectTool) { tint ->
        Icon(
            painter = painterResource(id = icon),
            contentDescription = stringResource(label),
            modifier = Modifier.size(cellSize - cellPadding * 2),
            tint = tint,
        )
    }
}

@Composable
private fun ToolIcon(
    icon: ImageVector,
    label: Int,
    tool: MachineEditMode,
    activeTool: MachineEditMode,
    onSelectTool: (MachineEditMode) -> Unit,
) {
    ToolIconBox(tool, activeTool, onSelectTool) { tint ->
        Icon(
            imageVector = icon,
            contentDescription = stringResource(label),
            modifier = Modifier.size(cellSize),
            tint = tint,
        )
    }
}

@Composable
private fun ToolIconBox(
    tool: MachineEditMode,
    activeTool: MachineEditMode,
    onSelectTool: (MachineEditMode) -> Unit,
    content: @Composable (tint: Color) -> Unit,
) {
    val isSelected = activeTool == tool
    Box(
        modifier =
            Modifier.size(cellSize)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
                .clickable { onSelectTool(tool) }
                .padding(cellPadding),
        contentAlignment = Alignment.Center,
    ) {
        val tint =
            if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        content(tint)
    }
}
