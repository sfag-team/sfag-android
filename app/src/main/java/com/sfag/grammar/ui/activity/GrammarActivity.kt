package com.sfag.grammar.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import com.sfag.grammar.presentation.viewmodel.GrammarViewModel
import com.sfag.grammar.presentation.viewmodel.InputsViewModel
import com.sfag.shared.icon.FileOpen
import com.sfag.shared.icon.FileSave as FileSaveIcon
import com.sfag.shared.theme.AppTheme
import com.sfag.grammar.ui.component.common.FilePicker
import com.sfag.grammar.ui.component.common.FileSave
import com.sfag.grammar.navigation.DestinationConstants
import com.sfag.grammar.ui.screen.BulkTestScreen
import com.sfag.grammar.ui.screen.GrammarScreen
import com.sfag.grammar.ui.screen.TestInputScreen


class GrammarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val assetPath = intent?.getStringExtra("example uri")
        setContent {
            AppTheme {
                // Create NavController for navigation
                val navController = rememberNavController()
                val grammarViewModel: GrammarViewModel = viewModel()
                val inputsViewModel: InputsViewModel = viewModel()
                LaunchedEffect(assetPath) {
                    if (assetPath != null) {
                        val inputStream = assets.open(assetPath)
                        grammarViewModel.loadFromXmlStream(inputStream)
                    }
                }
                // Set up NavHost with two composable screens
                Scaffold(
                    // Bottom navigation
                    topBar = {
                        TopNavigationBar(navController = navController, grammarViewModel, context = this)
                    }, content = { padding ->
                        // Nav host: where screens are placed
                        NavHostContainer(navController = navController, padding = padding, grammarViewModel, inputsViewModel)
                    }
                )
            }
        }
    }
}

@Composable
fun NavHostContainer(
    navController: NavHostController,
    padding: PaddingValues,
    grammarViewModel: GrammarViewModel,
    inputsViewModel: InputsViewModel,
) {

    NavHost(navController = navController, startDestination = "grammarScreen",
        modifier = Modifier.padding(paddingValues = padding)) {

        composable("grammarScreen") {
            GrammarScreen(grammarViewModel)
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
fun TopNavigationBar(navController: NavHostController, grammarViewModel: GrammarViewModel, context: Context) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    CenterAlignedTopAppBar(
        title = {

        },
        navigationIcon = {
            Row{
                IconButton(onClick = {
                    val intent = Intent().setClassName(
                        context,
                        "com.sfag.home.ui.activity.HomeActivity"
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
                Icon(FileOpen, contentDescription = "Open grammar from file", tint = MaterialTheme.colorScheme.primary)
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
            DestinationConstants.DestinationItems.forEach { navItem ->
                IconButton(
                    onClick = {
                        if (currentRoute != navItem.route && grammarViewModel.isGrammarFinished.value == true) {
                            navController.navigate(navItem.route)
                        }
                    }
                ) {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label,
                        tint = if (currentRoute?.startsWith(navItem.route) == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }

        }

    )

}
