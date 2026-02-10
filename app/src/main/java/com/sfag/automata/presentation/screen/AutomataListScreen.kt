package com.sfag.automata.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.automata.domain.model.machine.FiniteMachine
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.domain.usecase.isDeterministicFinite
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.TuringTransition
import com.sfag.automata.presentation.viewmodel.AutomataViewModel
import com.sfag.automata.presentation.viewmodel.CurrentMachine
import com.sfag.automata.presentation.component.widget.DefaultDialogWindow
import com.sfag.automata.presentation.component.widget.DefaultTextField
import com.sfag.automata.presentation.component.widget.ItemSpecificationIcon
import com.sfag.automata.data.JffParser
import com.sfag.shared.presentation.component.DefaultButton
import com.sfag.shared.presentation.component.ImmutableTextField

@Composable
fun AutomataListScreen(exampleMachine:Machine? = null, navBack: () -> Unit, navToAutomata: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AutomataViewModel = hiltViewModel()
    var createNewMachine by remember {
        mutableStateOf(false)
    }
    LaunchedEffect (Unit) {
        if(exampleMachine!=null&&CurrentMachine.machine==null){
            viewModel.saveMachine(machine = exampleMachine)
            CurrentMachine.machine = exampleMachine
            navToAutomata()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
        uri?.let { it ->
            val inputStream = context.contentResolver.openInputStream(it)
            val content = inputStream?.bufferedReader()?.use { it.readText() }
            inputStream?.close()

            content?.let { jffXml ->
                val (states, transitions) = JffParser.parseJff(jffXml)
                val typeTag = jffXml.split("<type>")[1].split("</type>")[0].trim().lowercase()
                val machineType: MachineType = MachineType.fromTag(typeTag)
                val machine = when (machineType) {
                    MachineType.Finite -> FiniteMachine(
                        name = "imported finite",
                        states = states.toMutableList(),
                        transitions = transitions.toMutableList()
                    )
                    MachineType.Pushdown -> PushDownMachine(
                        name = "imported pushdown",
                        states = states.toMutableList(),
                        transitions = transitions.filterIsInstance<PushDownTransition>().toMutableList()
                    )
                    MachineType.Turing -> TuringMachine(
                        name = "imported turing",
                        states = states.toMutableList(),
                        transitions = transitions.filterIsInstance<TuringTransition>().toMutableList()
                    )
                }
                viewModel.saveMachine(machine = machine)
                CurrentMachine.machine = machine
                navToAutomata()
            }
        }
    }
)


    BackHandler(enabled = true) {
        navBack()
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.size(8.dp))
        Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            DefaultButton(text = "Back", modifier = Modifier.fillMaxWidth(0.25f)) {
                navBack()
            }
            Spacer(modifier = Modifier.size(16.dp))
            ImmutableTextField(
                text = "Nice to meet you!",
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(MaterialTheme.shapes.medium),
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
        LazyColumn(
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(MaterialTheme.shapes.large)
                .border(4.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 16.dp)
        ) {
            items(viewModel.getAllMachinesName()) { item ->
                val machine = viewModel.getMachineByName(item) ?: return@items

                val detLabel = when (machine.machineType) {
                    MachineType.Finite ->
                        if (machine.isDeterministicFinite()) "DFA" else "NFA"
                    MachineType.Pushdown -> "PDA"
                    MachineType.Turing ->
                        "TM"
                }
                Spacer(modifier = Modifier.size(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(60.dp)
                        .clip(MaterialTheme.shapes.large)
                        .border(
                            3.dp,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.shapes.large
                        )
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                        val selectedMachine = viewModel.getMachineByName(item) ?: return@clickable
                        CurrentMachine.machine = selectedMachine
                        navToAutomata()
                    },
                    verticalAlignment = CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    ImmutableTextField(
                        text = "$item ($detLabel)",
                        textColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
        Row(
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = com.sfag.R.drawable.add_icon),
                contentDescription = "",
                modifier = Modifier
                    .clickable { createNewMachine = true }
                    .size(50.dp)
            )
            Spacer(modifier = Modifier.size(24.dp))
            DefaultButton(text = "Import") {
                filePickerLauncher.launch(arrayOf("application/octet-stream", "text/xml"))
            }
        }

    }
    key(createNewMachine) {
        if (createNewMachine) {
            NewMachineWindow { newMachine ->
                createNewMachine = false
                newMachine?.let {
                    viewModel.saveMachine(machine = it)
                    CurrentMachine.machine = it
                    navToAutomata()
                }
            }
        }
    }
}

@Composable
private fun NewMachineWindow(finished: (Machine?) -> Unit) {
    var name by remember {
        mutableStateOf("")
    }
    var type by remember {
        mutableStateOf<MachineType?>(null)
    }

    DefaultDialogWindow(
        title = "Create new machine",
        height = 350,
        conditionToEnable = name.isNotEmpty() && type != null,
        onDismiss = {
            finished(null)
        },
        onConfirm = {
            val selectedType = type
            if (name.isNotEmpty() && selectedType != null) {
                when (selectedType) {
                    MachineType.Finite -> {
                        finished(FiniteMachine(name = name))
                    }

                    MachineType.Pushdown -> {
                        finished(PushDownMachine(name = name))
                    }

                    MachineType.Turing -> {
                        finished(TuringMachine(name = name))
                    }
                }
            }
        }) {

        /**
         * Row for Finite or PushDown machine
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ItemSpecificationIcon(
                icon = com.sfag.R.drawable.pd_automata_icon,
                text = "Push Down",
                isActive = type?.equals(MachineType.Pushdown) ?: false
            ) {
                type = MachineType.Pushdown
            }

            ItemSpecificationIcon(
                icon = com.sfag.R.drawable.finite_automata_icon,
                text = "Finite",
                isActive = type?.equals(MachineType.Finite) ?: false
            ) {
                type = MachineType.Finite
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalAlignment = CenterVertically
        ) {
            DefaultTextField(
                hint = "name",
                value = name,
                requirementText = "",
                onTextChange = { name = it }) { true }
        }
    }
}
