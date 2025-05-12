package com.droidhen.formalautosim.presentation.activities

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.droidhen.formalautosim.core.entities.machines.FiniteMachine
import com.droidhen.formalautosim.core.entities.machines.Machine
import com.droidhen.formalautosim.core.entities.machines.MachineType
import com.droidhen.formalautosim.core.entities.machines.PushDownMachine
import com.droidhen.formalautosim.core.viewModel.AutomataViewModel
import com.droidhen.formalautosim.core.viewModel.CurrentMachine
import com.droidhen.formalautosim.data.local.ExternalStorageController
import views.DefaultFASDialogWindow
import views.FASButton
import views.FASDefaultTextField
import views.FASImmutableTextField
import views.ItemSpecificationIcon

@Composable
fun AutomataListScreen(navBack: () -> Unit, navToAutomata: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AutomataViewModel = hiltViewModel()
    var createNewMachine by remember {
        mutableStateOf(false)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val content = inputStream?.bufferedReader()?.use { it.readText() }
            inputStream?.close()

            content?.let { jffXml ->
                val (states, transitions) = ExternalStorageController().parseJff(jffXml)
                val machineType: MachineType =
                    if (jffXml.split("<type>").get(1).split("</type>").get(0)
                            .equals(MachineType.Finite.tag)
                    ) MachineType.Finite else MachineType.Pushdown
                val machine = if (machineType == MachineType.Finite) FiniteMachine(
                    name = "imported finite",
                    states = states.toMutableList(),
                    transitions = transitions.toMutableList()
                ) else PushDownMachine(name = "imported pushdown", states =states.toMutableList(), transitions =transitions.toMutableList() )
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
        FASImmutableTextField(
            text = "Nice to meet you!",
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(60.dp),
            fontSize = 30.sp
        )
        Spacer(modifier = Modifier.height(30.dp))
        LazyColumn(
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(MaterialTheme.shapes.large)
                .border(4.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(start = 16.dp)
        ) {
            items(viewModel.getAllMachinesName()) { item ->
                Spacer(modifier = Modifier.size(16.dp))
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
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(24.dp))
                    FASImmutableTextField(
                        text = item,
                        modifier = Modifier.clickable {
                            CurrentMachine.machine = viewModel.getMachineByName(item)!!
                            navToAutomata()
                        })
                }
            }
        }
        Row(
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = com.droidhen.theme.R.drawable.add_icon),
                contentDescription = "",
                modifier = Modifier
                    .clickable { createNewMachine = true }
                    .size(50.dp)
            )
            Spacer(modifier = Modifier.size(24.dp))
            FASButton(text = "Import") {
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

    DefaultFASDialogWindow(
        title = "Create new machine",
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
                icon = com.droidhen.theme.R.drawable.pd_automata_icon,
                text = "Push Down",
                isActive = type?.equals(MachineType.Pushdown) ?: false
            ) {
                type = MachineType.Pushdown
            }

            ItemSpecificationIcon(
                icon = com.droidhen.theme.R.drawable.finite_automata_icon,
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
            FASDefaultTextField(
                hint = "name",
                value = name,
                requirementText = "",
                onTextChange = { name = it }) { true }
        }
    }
}
