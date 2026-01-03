package com.sfag.grammar.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.sfag.grammar.core.viewmodel.GrammarViewModel
import com.sfag.grammar.data.local.GrammarFileStorage


@Composable
fun FilePicker(grammarViewModel: GrammarViewModel, navController: NavController) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            grammarViewModel.loadFromXmlUri(context, it)
        }
        navController.navigate("grammarScreen")
    }

    LaunchedEffect(Unit) {
        filePickerLauncher.launch("*/*")
    }
}

@Composable
fun FileSave(grammarViewModel: GrammarViewModel, navController: NavController) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml")
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
