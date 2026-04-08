package com.sfag.main

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sfag.R
import com.sfag.automata.data.AutomataStorage
import com.sfag.automata.data.Jff
import com.sfag.automata.data.toMachine
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.MachineType
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.ui.AutomataScreen
import com.sfag.automata.ui.AutomataViewModel
import com.sfag.grammar.ui.GrammarScreen
import com.sfag.grammar.ui.GrammarViewModel
import com.sfag.main.config.JFF_OPEN_MIME_TYPES
import com.sfag.main.ui.AboutScreen
import com.sfag.main.ui.ExamplesScreen
import com.sfag.main.ui.HomeScreen
import com.sfag.main.ui.component.CreateButton
import com.sfag.main.ui.component.DefaultDialog
import com.sfag.main.ui.component.DefaultSnackbarHost
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.ImportButton
import com.sfag.main.ui.component.ItemSpecificationIcon
import com.sfag.main.ui.component.PortraitPillarbox
import com.sfag.main.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

enum class Destinations(val route: String) {
    HOME("homeScreen"),
    AUTOMATA("automataScreen"),
    GRAMMAR("grammarScreen"),
    EXAMPLES("examplesScreen"),
    ABOUT("aboutScreen"),
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var automataStorage: AutomataStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        configureScreenOrientation()

        setContent {
            AppTheme {
                PortraitPillarbox {
                    val navController = rememberNavController()
                    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        innerPadding ->
                        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = Destinations.HOME.route,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                composable(route = Destinations.HOME.route) {
                                    var showNewMachineDialog by remember { mutableStateOf(false) }

                                    val initImportLauncher =
                                        rememberLauncherForActivityResult(
                                            contract = ActivityResultContracts.OpenDocument()
                                        ) { uri ->
                                            showNewMachineDialog = false
                                            if (uri == null) {
                                                return@rememberLauncherForActivityResult
                                            }
                                            navController.navigate(
                                                "${Destinations.AUTOMATA.route}?importUri=${
                                                Uri.encode(
                                                    uri.toString()
                                                )
                                            }"
                                            ) {
                                                launchSingleTop = true
                                            }
                                        }

                                    HomeScreen(
                                        navToAutomata = {
                                            if (automataStorage.hasStoredMachine()) {
                                                navController.navigate(
                                                    Destinations.AUTOMATA.route
                                                ) {
                                                    launchSingleTop = true
                                                }
                                            } else {
                                                showNewMachineDialog = true
                                            }
                                        },
                                        navToGrammar = {
                                            navController.navigate(Destinations.GRAMMAR.route) {
                                                launchSingleTop = true
                                            }
                                        },
                                        navToExamples = {
                                            navController.navigate(Destinations.EXAMPLES.route) {
                                                launchSingleTop = true
                                            }
                                        },
                                        navToAbout = {
                                            navController.navigate(Destinations.ABOUT.route) {
                                                launchSingleTop = true
                                            }
                                        },
                                    )

                                    if (showNewMachineDialog) {
                                        NewMachineDialog(
                                            onDismiss = { showNewMachineDialog = false },
                                            onImport = {
                                                initImportLauncher.launch(JFF_OPEN_MIME_TYPES)
                                            },
                                            onConfirm = { name, machineType ->
                                                showNewMachineDialog = false
                                                navController.navigate(
                                                    "${Destinations.AUTOMATA.route}?name=${
                                                        Uri.encode(
                                                            name
                                                        )
                                                    }&machineType=${machineType.name}"
                                                ) {
                                                    launchSingleTop = true
                                                }
                                            },
                                        )
                                    }
                                }
                                composable(route = Destinations.EXAMPLES.route) {
                                    ExamplesScreen(
                                        navBack = { navController.popBackStack() },
                                        navToGrammar = { examplePath, _ ->
                                            navController.navigate(
                                                "${Destinations.GRAMMAR.route}?examplePath=${
                                                    Uri.encode(
                                                        examplePath
                                                    )
                                                }"
                                            ) {
                                                popUpTo(Destinations.HOME.route) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        },
                                        navToAutomata = { examplePath, name ->
                                            navController.navigate(
                                                "${Destinations.AUTOMATA.route}?name=${
                                                    Uri.encode(
                                                        name
                                                    )
                                                }&examplePath=${
                                                    Uri.encode(
                                                        examplePath
                                                    )
                                                }"
                                            ) {
                                                popUpTo(Destinations.HOME.route) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                }
                                composable(route = Destinations.ABOUT.route) {
                                    AboutScreen(navBack = { navController.popBackStack() })
                                }
                                composable(
                                    route =
                                        "${Destinations.AUTOMATA.route}?name={name}&machineType={machineType}&examplePath={examplePath}&importUri={importUri}",
                                    arguments =
                                        listOf(
                                            navArgument("name") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            },
                                            navArgument("machineType") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            },
                                            navArgument("examplePath") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            },
                                            navArgument("importUri") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            },
                                        ),
                                ) { backStackEntry ->
                                    val name = backStackEntry.arguments?.getString("name")
                                    val machineType =
                                        backStackEntry.arguments?.getString("machineType")
                                    val examplePath =
                                        backStackEntry.arguments?.getString("examplePath")
                                    val importUri = backStackEntry.arguments?.getString("importUri")

                                    val context = LocalContext.current
                                    val viewModel: AutomataViewModel = hiltViewModel()

                                    // Load synchronously during first composition - no extra frames
                                    var initialized by remember {
                                        mutableStateOf(
                                            if (machineType != null) {
                                                val machine =
                                                    when (machineType) {
                                                        MachineType.FINITE.name ->
                                                            FiniteMachine(name = name ?: "")

                                                        MachineType.PUSHDOWN.name ->
                                                            PushdownMachine(name = name ?: "")

                                                        else ->
                                                            throw IllegalArgumentException(
                                                                "Unknown machine type: $machineType"
                                                            )
                                                    }
                                                viewModel.setCurrentMachine(machine)
                                                true
                                            } else if (importUri != null) {
                                                try {
                                                    val contentUri = importUri.toUri()
                                                    val fileName =
                                                        context.contentResolver
                                                            .query(
                                                                contentUri,
                                                                arrayOf(
                                                                    OpenableColumns.DISPLAY_NAME
                                                                ),
                                                                null,
                                                                null,
                                                                null,
                                                            )
                                                            ?.use { cursor ->
                                                                if (cursor.moveToFirst()) {
                                                                    cursor
                                                                        .getString(0)
                                                                        ?.substringBeforeLast(".")
                                                                } else {
                                                                    null
                                                                }
                                                            } ?: ""
                                                    context.contentResolver
                                                        .openInputStream(contentUri)
                                                        ?.use { stream ->
                                                            val jff = Jff.parse(stream)
                                                            viewModel.setCurrentMachine(
                                                                jff.toMachine(fileName),
                                                                jff.positions,
                                                            )
                                                        }
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "MainActivity",
                                                        "Failed to import file",
                                                        e,
                                                    )
                                                }
                                                true
                                            } else if (examplePath == null) {
                                                viewModel.loadMachine()
                                                true
                                            } else {
                                                false
                                            }
                                        )
                                    }

                                    LaunchedEffect(Unit) {
                                        if (initialized) {
                                            return@LaunchedEffect
                                        }
                                        examplePath ?: return@LaunchedEffect
                                        val machineName = name ?: ""
                                        if (
                                            viewModel.loadMachine() && viewModel.hasUnsavedChanges
                                        ) {
                                            viewModel.pendingExampleUri = examplePath
                                            viewModel.pendingExampleName = machineName
                                        } else {
                                            try {
                                                val jff =
                                                    context.assets.open(examplePath).use {
                                                        Jff.parse(it)
                                                    }
                                                viewModel.setCurrentMachine(
                                                    jff.toMachine(machineName),
                                                    jff.positions,
                                                )
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "MainActivity",
                                                    "Failed to load example: $examplePath",
                                                    e,
                                                )
                                            }
                                        }
                                        initialized = true
                                    }

                                    DisposableEffect(backStackEntry) {
                                        val observer = LifecycleEventObserver { _, event ->
                                            if (event == Lifecycle.Event.ON_STOP) {
                                                viewModel.currentMachine?.let {
                                                    viewModel.autoSave(it)
                                                }
                                            }
                                        }
                                        backStackEntry.lifecycle.addObserver(observer)
                                        onDispose {
                                            backStackEntry.lifecycle.removeObserver(observer)
                                        }
                                    }

                                    if (!initialized) {
                                        return@composable
                                    }

                                    val snackbarHostState = remember { SnackbarHostState() }
                                    Scaffold(
                                        containerColor =
                                            MaterialTheme.colorScheme.surfaceContainerLowest,
                                        snackbarHost = { DefaultSnackbarHost(snackbarHostState) },
                                    ) { automataInnerPadding ->
                                        if (viewModel.currentMachine != null) {
                                            AutomataScreen(
                                                modifier =
                                                    Modifier.fillMaxSize()
                                                        .padding(automataInnerPadding),
                                                snackbarHostState = snackbarHostState,
                                                navBack = { navController.popBackStack() },
                                            )
                                        }
                                    }
                                }
                                composable(
                                    route =
                                        "${Destinations.GRAMMAR.route}?examplePath={examplePath}",
                                    arguments =
                                        listOf(
                                            navArgument("examplePath") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            }
                                        ),
                                ) { backStackEntry ->
                                    val examplePath =
                                        backStackEntry.arguments?.getString("examplePath")
                                    val context = LocalContext.current
                                    val viewModel: GrammarViewModel = hiltViewModel()

                                    var initialized by remember {
                                        mutableStateOf(
                                            if (viewModel.rules.isNotEmpty()) {
                                                true
                                            } else if (examplePath == null) {
                                                viewModel.loadGrammar()
                                                true
                                            } else {
                                                false
                                            }
                                        )
                                    }

                                    LaunchedEffect(Unit) {
                                        if (initialized) {
                                            return@LaunchedEffect
                                        }
                                        examplePath ?: return@LaunchedEffect
                                        try {
                                            context.assets.open(examplePath).use {
                                                viewModel.loadFromJffStream(it)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "MainActivity",
                                                "Failed to load grammar example: $examplePath",
                                                e,
                                            )
                                        }
                                        initialized = true
                                    }

                                    DisposableEffect(backStackEntry) {
                                        val observer = LifecycleEventObserver { _, event ->
                                            if (event == Lifecycle.Event.ON_STOP) {
                                                viewModel.autoSave()
                                            }
                                        }
                                        backStackEntry.lifecycle.addObserver(observer)
                                        onDispose {
                                            backStackEntry.lifecycle.removeObserver(observer)
                                        }
                                    }

                                    if (!initialized) {
                                        return@composable
                                    }

                                    GrammarScreen(navBack = { navController.popBackStack() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewMachineDialog(
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onConfirm: (machineName: String, machineType: MachineType) -> Unit,
) {
    var machineName by remember { mutableStateOf("") }
    var machineType by remember { mutableStateOf<MachineType?>(null) }

    DefaultDialog(
        onDismissRequest = onDismiss,
        buttons = {
            ImportButton(onClick = onImport, modifier = Modifier.weight(1f))
            CreateButton(
                onClick = { onConfirm(machineName, machineType!!) },
                modifier = Modifier.weight(1f),
                enabled = machineType != null && machineName.isNotBlank(),
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemSpecificationIcon(
                icon = R.drawable.finite_automata,
                text = stringResource(R.string.finite_automaton),
                isActive = machineType == MachineType.FINITE,
            ) {
                machineType = MachineType.FINITE
            }
            ItemSpecificationIcon(
                icon = R.drawable.pushdown_automata,
                text = stringResource(R.string.pushdown_automaton),
                isActive = machineType == MachineType.PUSHDOWN,
            ) {
                machineType = MachineType.PUSHDOWN
            }
        }

        DefaultTextField(
            value = machineName,
            onValueChange = { machineName = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.machine_name),
            suffix = ".jff",
        )
    }
}
