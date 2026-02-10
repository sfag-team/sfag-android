package com.sfag.grammar.presentation.component.widget

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.sfag.grammar.presentation.viewmodel.GrammarViewModel
import com.sfag.grammar.data.GrammarFileStorage

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
