package com.sfag.automata.ui.component.edit

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.presentation.model.AcceptanceCriteria
import com.sfag.R
import com.sfag.automata.ui.component.common.DefaultTextField
import com.sfag.automata.ui.component.common.DropDownSelector
import com.sfag.shared.ui.component.DefaultButton
import com.sfag.shared.ui.component.ImmutableTextField


/**
 * Screen for editing input bar content
 *
 * @param finishedEditing it's a lambda - that invokes when user confirm his changes
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun Machine.EditingInput(finishedEditing: () -> Unit) {
    val inputValue = mutableStateOf(input.toString())
    var editingRecompose by remember { mutableIntStateOf(0) }

    key(editingRecompose) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.size(24.dp))
            Row(modifier = Modifier.fillMaxWidth(0.9f)) {
                Text(
                    text = stringResource(R.string.editing_input_headline),
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.width(10.dp))
                DefaultButton(text = "ADD") {
                    savedInputs.add(input)
                    input = java.lang.StringBuilder(savedInputs.last().toString()+"")
                    editingRecompose++
                }
            }

            if (machineType == MachineType.Pushdown) {
                this@EditingInput as PushDownMachine
                val listOfCriteria = listOf(
                    AcceptanceCriteria.BY_FINITE_STATE.text,
                    AcceptanceCriteria.BY_INITIAL_STACK.text
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(fontSize = 24.sp, text = "Accept input by reaching:")
                Spacer(modifier = Modifier.size(4.dp))
                DropDownSelector(
                    items = listOfCriteria,
                    defaultSelectedIndex = listOfCriteria.indexOf(acceptanceCriteria.text)
                ) { newCriteria ->
                    acceptanceCriteria =
                        if (newCriteria.toString() == AcceptanceCriteria.BY_FINITE_STATE.text) AcceptanceCriteria.BY_FINITE_STATE else AcceptanceCriteria.BY_INITIAL_STACK
                }

            }


            Spacer(modifier = Modifier.size(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DefaultTextField(
                    hint = "",
                    value = inputValue.value,
                    requirementText = stringResource(R.string.requirement_text_for_machine_input),
                    onTextChange = { newInput ->
                        input.clear()
                        input.append(newInput)
                        inputValue.value = newInput
                        imuInput = StringBuilder(input.toString())
                        fullInputSnapshot = input.toString()
                    }) {
                    input.contains("^[A-Za-z]+$".toRegex())
                }
                savedInputs.forEach { input ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.Center,
                        CenterVertically
                    ) {
                        ImmutableTextField(text = input.toString(),
                            modifier = Modifier
                                .clickable {
                                    this@EditingInput.input = StringBuilder(input.toString())
                                    imuInput = StringBuilder(input.toString())
                                    fullInputSnapshot = this@EditingInput.input.toString()
                                    setInitialStateAsCurrent()
                                    editingRecompose++
                                }
                                .width(200.dp)
                                .height(40.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(painter = painterResource(id = R.drawable.bin),
                            contentDescription = "",
                            modifier = Modifier
                                .clickable {
                                    savedInputs.remove(input)
                                    editingRecompose++
                                }
                                .size(30.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(
                                id =
                                if(machineType == MachineType.Finite || ((this@EditingInput as PushDownMachine).acceptanceCriteria == AcceptanceCriteria.BY_FINITE_STATE)){
                                    if (canReachFinalState(input, true)) {
                                        com.sfag.R.drawable.check
                                    } else {
                                        com.sfag.R.drawable.cross
                                    }
                                } else {
                                    if (canReachInitialStackPDA(input, true, listOf('Z'))) {
                                        com.sfag.R.drawable.check
                                    } else {
                                        com.sfag.R.drawable.cross
                                    }
                                }

                            ),
                            contentDescription = "",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                }
            }
            Spacer(modifier = Modifier.size(16.dp))
            DefaultButton(text = "Confirm", modifier = Modifier.width(130.dp), onClick = finishedEditing)
        }
    }


}
