package com.example.gramatika

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gramatika.ui.theme.GramatikaTheme
import com.example.gramatika.ui.theme.File_open
import com.example.gramatika.ui.theme.File_save

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GramatikaTheme {
                // Create NavController for navigation
                val navController = rememberNavController()
                val grammarViewModel: Grammar = viewModel()
                val inputsViewModel: Inputs = viewModel()
                // Set up NavHost with two composable screens

                Scaffold(
                    // Bottom navigation
                    topBar = {
                        TopNavigationBar(navController = navController)
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
    grammarViewModel: Grammar,
    inputsViewModel: Inputs
) {

    NavHost(navController = navController, startDestination = "grammarScreen",
        modifier = Modifier.padding(paddingValues = padding)) {

        composable("grammarScreen") {
            GrammarScreen(grammarViewModel) // GrammarScreen composable
        }
        composable("bulkTestScreen") {
            BulkTestScreen(navController, grammarViewModel, inputsViewModel)
        }
        composable(
            "testScreen?input={input}",
            arguments = listOf(
                navArgument("input") {
                    type = NavType.StringType
                    defaultValue = "" // Default empty input
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val input = backStackEntry.arguments?.getString("input") ?: ""
            TestInputScreen(grammarViewModel, preInput = input)
        }
        composable("filePick") {
            FilePicker(grammarViewModel,navController)
        }
        composable("fileSave"){
            FileSave(grammarViewModel, navController)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    CenterAlignedTopAppBar(
        title = {

        },
        navigationIcon = {
            Row{
            IconButton(
                onClick = {
                    navController.navigate("filePick")
                }
            ) {
                Icon(File_open, contentDescription = "Open grammar from file", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = {
                    navController.navigate("fileSave")
                }
            ){
                Icon(File_save, contentDescription = "Save Grammar to a file", tint = MaterialTheme.colorScheme.primary)
            }}
        },
        actions = {
            Constants.TopNavItems.forEach { navItem ->
                IconButton(
                    onClick = {
                        if (currentRoute != navItem.route) {
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
