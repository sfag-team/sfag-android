package com.example.gramatika

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
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

class GrammarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val assetPath = intent?.getStringExtra("example uri")
        setContent {
            GramatikaTheme {
                // Create NavController for navigation
                val navController = rememberNavController()
                val grammarViewModel: Grammar = viewModel()
                val inputsViewModel: Inputs = viewModel()
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
    grammarViewModel: Grammar,
    inputsViewModel: Inputs,
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
            FilePicker(grammarViewModel,navController)
        }
        composable("fileSave"){
            FileSave(grammarViewModel, navController)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar(navController: NavHostController, grammarViewModel: Grammar, context: Context) {
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
                        "com.droidhen.formalautosim.presentation.activities.MainActivity"
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
            NavConstants.TopNavItems.forEach { navItem ->
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
