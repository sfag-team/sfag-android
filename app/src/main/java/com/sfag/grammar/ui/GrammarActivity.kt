package com.sfag.grammar.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.sfag.grammar.data.GrammarFileStorage
import com.sfag.shared.ui.component.DefaultSnackbarHost
import com.sfag.shared.ui.component.FileOpenIcon
import com.sfag.shared.ui.component.FileSaveIcon
import com.sfag.shared.ui.configureScreenOrientation
import com.sfag.shared.ui.theme.AppTheme

enum class GrammarDestinations(val route: String) {
    GRAMMAR("grammarScreen"),
    TEST("testScreen"),
    BULK_TEST("bulkTestScreen")
}

class GrammarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureScreenOrientation()
        val assetPath = intent?.getStringExtra("example uri")
        setContent {
            AppTheme {
                // Create NavController for navigation
                val navController = rememberNavController()
                val grammarViewModel: GrammarViewModel = viewModel()
                val inputsViewModel: BulkTestViewModel = viewModel()
                LaunchedEffect(assetPath) {
                    if (assetPath != null) {
                        val inputStream = assets.open(assetPath)
                        grammarViewModel.loadFromXmlStream(inputStream)
                    }
                }
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    topBar = {
                        TopNavigationBar(navController = navController, grammarViewModel, context = this)
                    },
                    snackbarHost = { DefaultSnackbarHost(snackbarHostState) },
                    content = { padding ->
                        NavHostContainer(navController = navController, padding = padding, grammarViewModel, inputsViewModel, snackbarHostState)
                    }
                )
            }
        }
    }
}

@Composable
private fun NavHostContainer(
    navController: NavHostController,
    padding: PaddingValues,
    grammarViewModel: GrammarViewModel,
    inputsViewModel: BulkTestViewModel,
    snackbarHostState: SnackbarHostState,
) {

    NavHost(navController = navController, startDestination = "grammarScreen",
        modifier = Modifier.padding(paddingValues = padding)) {
        composable("grammarScreen") {
            GrammarScreen(grammarViewModel, snackbarHostState)
        }
        composable("bulkTestScreen") {
            BulkTestScreen(navController, grammarViewModel, inputsViewModel)
        }
        composable(
            "testScreen?input={input}",
            arguments = listOf(
                navArgument("input") {
                    type = NavType.StringType
                    defaultValue = "." // Default empty input
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val input = backStackEntry.arguments?.getString("input") ?: "."
            TestInputScreen(grammarViewModel, preInput = input)
        }
        composable("filePick") {
            FilePicker(grammarViewModel, navController)
        }
        composable("fileSave"){
            FileSave(grammarViewModel, navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopNavigationBar(navController: NavHostController, grammarViewModel: GrammarViewModel, context: Context) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {
            Row{
                IconButton(onClick = {
                    val intent = Intent().setClassName(
                        context,
                        "com.sfag.home.ui.HomeActivity"
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Home, contentDescription = "Return to Home", tint = MaterialTheme.colorScheme.primary)
                }
            IconButton(
                onClick = {
                    navController.navigate("filePick")
                }
            ) {
                Icon(FileOpenIcon, contentDescription = "Open grammar from file", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = {
                    navController.navigate("fileSave")
                }
            ){
                Icon(FileSaveIcon, contentDescription = "Save Grammar to a file", tint = MaterialTheme.colorScheme.primary)
            }}
        },
        actions = {
            data class NavAction(val dest: GrammarDestinations, val icon: ImageVector, val label: String)
            val navActions = listOf(
                NavAction(GrammarDestinations.GRAMMAR, Icons.Default.Build, "Grammar"),
                NavAction(GrammarDestinations.TEST, Icons.Default.PlayArrow, "Test"),
                NavAction(GrammarDestinations.BULK_TEST, Icons.AutoMirrored.Filled.List, "Bulk Test")
            )
            navActions.forEach { navItem ->
                IconButton(
                    onClick = {
                        if (currentRoute != navItem.dest.route && grammarViewModel.isGrammarFinished) {
                            navController.navigate(navItem.dest.route)
                        }
                    }
                ) {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label,
                        tint = if (currentRoute?.startsWith(navItem.dest.route) == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
        }
    )
}

@Composable
fun FilePicker(grammarViewModel: GrammarViewModel, navController: NavController) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            grammarViewModel.loadFromXmlUri(context, it)
        }
        navController.navigate("grammarScreen")
    }

    LaunchedEffect(Unit) {
        filePickerLauncher.launch(arrayOf("application/octet-stream", "text/xml"))
    }
}

@Composable
fun FileSave(grammarViewModel: GrammarViewModel, navController: NavController) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            GrammarFileStorage.saveToJff(grammarViewModel.getIndividualRules(), context, it)
        }
        navController.navigate("grammarScreen")
    }

    LaunchedEffect(Unit) {
        launcher.launch("grammar.jff")
    }
}
