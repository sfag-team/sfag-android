package com.sfag.automata.ui.screen

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
import com.sfag.automata.core.machine.FiniteMachine
import com.sfag.automata.core.machine.Machine
import com.sfag.automata.core.machine.MachineType
import com.sfag.automata.core.machine.PushDownMachine
import com.sfag.automata.core.transition.PushDownTransition
import com.sfag.automata.core.viewmodels.AutomataViewModel
import com.sfag.automata.core.viewmodels.CurrentMachine
import com.sfag.automata.theme.perlamutr_white
import com.sfag.automata.ui.common.DefaultDialogWindow
import com.sfag.automata.ui.common.DefaultTextField
import com.sfag.automata.ui.common.ImmutableTextField
import com.sfag.automata.ui.common.ItemSpecificationIcon
import com.sfag.shared.data.local.FileStorage
import com.sfag.shared.ui.common.DefaultButton


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
                val (states, transitions) = FileStorage.parseJff(jffXml)
                val machineType: MachineType =
                    if (jffXml.split("<type>")[1].split("</type>")[0] == MachineType.Finite.tag
                    ) MachineType.Finite else MachineType.Pushdown
                val machine = if (machineType == MachineType.Finite) FiniteMachine(
                    name = "imported finite",
                    states = states.toMutableList(),
                    transitions = transitions.toMutableList()
                ) else PushDownMachine(name = "imported pushdown", states =states.toMutableList(), transitions =transitions.filterIsInstance<PushDownTransition>().toMutableList() )
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
        Spacer(Modifier.size(30.dp))
        Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            DefaultButton(text = "Back", modifier = Modifier.fillMaxWidth(0.25f)) {
                navBack()
            }
            Spacer(modifier = Modifier.size(16.dp))
            ImmutableTextField(
                text = "Nice to meet you!",
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .background(perlamutr_white)
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
                .background(perlamutr_white)
                .padding(start = 16.dp)
        ) {
            items(viewModel.getAllMachinesName()) { item ->
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
                        CurrentMachine.machine = viewModel.getMachineByName(item)!!
                        navToAutomata()
                    },
                    verticalAlignment = CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    ImmutableTextField(text = item, textColor = perlamutr_white)
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
                painter = painterResource(id = com.sfag.theme.R.drawable.add_icon),
                contentDescription = "",
                modifier = Modifier
                    .clickable { createNewMachine = true }
                    .size(50.dp)
            )
            Spacer(modifier = Modifier.size(24.dp))
            DefaultButton(text = "Import") {
                filePickerLauncher.launch(arrayOf("text/*", "application/xml"))
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
            if (name.isNotEmpty() && type != null) {
                when (type!!) {
                    MachineType.Finite -> {
                        finished(FiniteMachine(name = name))
                    }

                    MachineType.Pushdown -> {
                        finished(PushDownMachine(name = name))
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
                icon = com.sfag.theme.R.drawable.pd_automata_icon,
                text = "Push Down",
                isActive = type?.equals(MachineType.Pushdown) ?: false
            ) {
                type = MachineType.Pushdown
            }

            ItemSpecificationIcon(
                icon = com.sfag.theme.R.drawable.finite_automata_icon,
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
