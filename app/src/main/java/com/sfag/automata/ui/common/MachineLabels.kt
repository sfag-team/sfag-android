package com.sfag.automata.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sfag.R
import com.sfag.automata.domain.common.isDeterministic
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine

@Composable
fun Machine.machineLabel(): String {
    val typeLabel =
        stringResource(
            when (this) {
                is FiniteMachine -> R.string.fa_label
                is PushdownMachine -> R.string.pda_label
                is TuringMachine -> R.string.tm_label
            }
        )
    val prefixRes =
        when (isDeterministic()) {
            true -> R.string.determinism_prefix
            false -> R.string.nondeterminism_prefix
            null -> return typeLabel
        }
    return stringResource(prefixRes) + typeLabel
}
