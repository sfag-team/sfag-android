package com.sfag.automata.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.State
import com.sfag.main.ui.component.CancelButton
import com.sfag.main.ui.component.ConfirmButton
import com.sfag.main.ui.component.DefaultDialog
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.ItemSpecificationIcon
import kotlinx.coroutines.launch

/** Dialog for creating or editing a state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Machine.StateDialog(
    selectedState: State?,
    tapOffset: Offset,
    onAddPosition: (stateIndex: Int, offset: Offset) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var stateName by remember { mutableStateOf(selectedState?.name ?: "") }
    var isInitial by remember { mutableStateOf(selectedState?.initial ?: false) }
    var isFinal by remember { mutableStateOf(selectedState?.final ?: false) }

    var tooltipMsg by remember { mutableIntStateOf(R.string.duplicate_state_name) }
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    DefaultDialog(
        onDismissRequest = onDismiss,
        buttons = {
            CancelButton(onClick = onDismiss)
            ConfirmButton(
                onClick = {
                    val isDuplicate =
                        states.any { it.name == stateName && it.index != selectedState?.index }
                    if (isDuplicate) {
                        scope.launch { tooltipState.show() }
                    } else {
                        if (selectedState == null) {
                            val newIndex = findNewStateIndex()
                            addNewState(
                                State(
                                    name = stateName,
                                    isCurrent = false,
                                    index = newIndex,
                                    final = isFinal,
                                    initial = isInitial,
                                )
                            )
                            onAddPosition(newIndex, tapOffset)
                        } else {
                            selectedState.name = stateName
                            selectedState.initial = isInitial
                            selectedState.final = isFinal
                        }
                        onConfirm()
                    }
                },
                enabled = stateName.isNotEmpty(),
            )
        },
    ) {
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(stringResource(tooltipMsg)) } },
            state = tooltipState,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = CenterVertically,
            ) {
                ItemSpecificationIcon(
                    icon = R.drawable.initial_state,
                    text = stringResource(R.string.initial_state),
                    isActive = isInitial,
                ) {
                    if (selectedState?.initial == true || !states.any { it.initial }) {
                        isInitial = !isInitial
                    } else {
                        tooltipMsg = R.string.initial_state_exists
                        scope.launch { tooltipState.show() }
                    }
                }

                ItemSpecificationIcon(
                    icon = R.drawable.final_state,
                    text = stringResource(R.string.final_state),
                    isActive = isFinal,
                ) {
                    isFinal = !isFinal
                }
            }
        }

        DefaultTextField(
            value = stateName,
            onValueChange = { if (it.length <= 5) stateName = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.state_name),
        )
    }
}
