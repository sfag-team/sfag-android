package com.sfag.automata.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.State

@Composable
fun Machine.StateList(
    recomposeKey: MutableIntState,
    onClickState: (State) -> Unit,
    onRemoveState: ((State) -> Unit)?,
) {
    key(recomposeKey.intValue) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.machine_state), style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(states) { state ->
                    val initialLabel = stringResource(R.string.initial_state)
                    val finalLabel = stringResource(R.string.final_state)
                    ListRow(
                        text =
                            state.name +
                                (if (state.initial) "  |  $initialLabel" else "") +
                                (if (state.final) "  |  $finalLabel" else ""),
                        onClick = { onClickState(state) },
                        onRemove = onRemoveState?.let { { it(state) } },
                    )
                }
            }
        }
    }
}
