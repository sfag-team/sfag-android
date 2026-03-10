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
import com.sfag.automata.domain.machine.Transition

@Composable
fun Machine.TransitionList(
    recomposeKey: MutableIntState,
    onClickTransition: (Transition) -> Unit,
    onRemoveTransition: ((Transition) -> Unit)?,
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
            Text(stringResource(R.string.machine_transition), style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(transitions) { transition ->
                    ListRow(
                        text =
                            "${getStateByIndex(
                                transition.fromState,
                            ).name} -> ${getStateByIndex(transition.toState).name}  |  ${transition.displayLabel()}",
                        onClick = { onClickTransition(transition) },
                        onRemove = onRemoveTransition?.let { { it(transition) } },
                    )
                }
            }
        }
    }
}
