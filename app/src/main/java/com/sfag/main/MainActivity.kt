package com.sfag.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sfag.R
import com.sfag.automata.AutomataActivity
import com.sfag.grammar.GrammarActivity
import com.sfag.main.config.EXTRA_EXAMPLE_NAME
import com.sfag.main.config.EXTRA_EXAMPLE_URI
import com.sfag.main.config.EXTRA_NEW_MACHINE_NAME
import com.sfag.main.config.EXTRA_NEW_MACHINE_TYPE
import com.sfag.main.config.EXTRA_OPEN_FILE_PICKER
import com.sfag.main.ui.AboutScreen
import com.sfag.main.ui.ExamplesScreen
import com.sfag.main.ui.HomeScreen
import com.sfag.main.ui.component.DefaultButton
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.ItemSpecificationIcon
import com.sfag.main.ui.component.PortraitPillarbox
import com.sfag.main.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

enum class Destinations(
    val route: String,
) {
    HOME("homeScreen"),
    EXAMPLES("examplesScreen"),
    ABOUT("aboutScreen"),
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        configureScreenOrientation()

        setContent {
            AppTheme {
                PortraitPillarbox {
                    val navController = rememberNavController()
                    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = Destinations.HOME.route,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                composable(route = Destinations.HOME.route) {
                                    var isNewMachineDialogShown by remember { mutableStateOf(false) }
                                    HomeScreen(
                                        navToAutomata = {
                                            if (hasSavedMachine()) {
                                                navToAutomataActivity(null)
                                            } else {
                                                isNewMachineDialogShown = true
                                            }
                                        },
                                        navToGrammar = { navToGrammarActivity(null) },
                                        navToExamplesScreen = {
                                            navController.navigate(Destinations.EXAMPLES.route)
                                        },
                                        navToAbout = { navController.navigate(Destinations.ABOUT.route) }
                                    )
                                    if (isNewMachineDialogShown) {
                                        NewMachineDialog(
                                            onDismiss = { isNewMachineDialogShown = false },
                                            onImport = {
                                                isNewMachineDialogShown = false
                                                navToAutomataActivity(null, importMode = true)
                                            },
                                            onConfirm = { type, name ->
                                                isNewMachineDialogShown = false
                                                navToNewAutomataActivity(type, name)
                                            }
                                        )
                                    }
                                }
                                composable(route = Destinations.EXAMPLES.route) {
                                    ExamplesScreen(
                                        navBack = {
                                            navController.navigate(Destinations.HOME.route) {
                                                popUpTo(Destinations.HOME.route) {
                                                    inclusive = true
                                                }
                                            }
                                        },
                                        navToGrammar = { grammarUri, name ->
                                            navController.navigate(Destinations.HOME.route) {
                                                popUpTo(Destinations.HOME.route) {
                                                    inclusive = true
                                                }
                                            }
                                            navToGrammarActivity(grammarUri, name)
                                        },
                                        navToAutomata = { automataUri, name ->
                                            navController.navigate(Destinations.HOME.route) {
                                                popUpTo(Destinations.HOME.route) {
                                                    inclusive = true
                                                }
                                            }
                                            navToAutomataActivity(automataUri, name)
                                        }
                                    )
                                }
                                composable(route = Destinations.ABOUT.route) {
                                    AboutScreen(navBack = { navController.navigate(Destinations.HOME.route) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navToGrammarActivity(
        uri: String?,
        name: String? = null,
    ) {
        val intent = Intent(this, GrammarActivity::class.java)
        uri?.let { intent.putExtra(EXTRA_EXAMPLE_URI, uri) }
        name?.let { intent.putExtra(EXTRA_EXAMPLE_NAME, it) }
        startActivity(intent)
    }

    private fun hasSavedMachine(): Boolean = File(filesDir, "automata/__current.jff").exists()

    private fun navToAutomataActivity(
        uri: String?,
        name: String? = null,
        importMode: Boolean = false,
    ) {
        val intent = Intent(this, AutomataActivity::class.java)
        uri?.apply { intent.putExtra(EXTRA_EXAMPLE_URI, this) }
        name?.let { intent.putExtra(EXTRA_EXAMPLE_NAME, it) }
        if (importMode) intent.putExtra(EXTRA_OPEN_FILE_PICKER, true)
        startActivity(intent)
    }

    private fun navToNewAutomataActivity(
        type: String,
        name: String,
    ) {
        val intent = Intent(this, AutomataActivity::class.java)
        intent.putExtra(EXTRA_NEW_MACHINE_TYPE, type)
        intent.putExtra(EXTRA_NEW_MACHINE_NAME, name)
        startActivity(intent)
    }
}

@Composable
private fun NewMachineDialog(
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onConfirm: (machineType: String, machineName: String) -> Unit,
) {
    var machineType by remember { mutableStateOf<String?>(null) }
    var machineName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ItemSpecificationIcon(
                        icon = R.drawable.finite_automata,
                        text = stringResource(R.string.finite_automaton),
                        isActive = machineType == "finite",
                    ) {
                        machineType = "finite"
                    }
                    ItemSpecificationIcon(
                        icon = R.drawable.pushdown_automata,
                        text = stringResource(R.string.pushdown_automaton),
                        isActive = machineType == "pushdown",
                    ) {
                        machineType = "pushdown"
                    }
                }

                DefaultTextField(
                    label = stringResource(R.string.machine_name),
                    value = machineName,
                    suffix = ".jff",
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { machineName = it },
                )

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DefaultButton(
                        text = stringResource(R.string.import_file),
                        modifier = Modifier.weight(1f),
                        height = 40.dp,
                    ) {
                        onImport()
                    }
                    DefaultButton(
                        text = stringResource(R.string.create_button),
                        modifier = Modifier.weight(1f),
                        height = 40.dp,
                        enabled = machineType != null,
                    ) {
                        onConfirm(machineType!!, machineName)
                    }
                }
            }
        }
    }
}
