package com.sfag.grammar.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.R
import com.sfag.grammar.data.exportToJff
import com.sfag.grammar.ui.edit.GrammarEditor
import com.sfag.grammar.ui.input.MultiInputView
import com.sfag.grammar.ui.input.MultiInputViewModel
import com.sfag.grammar.ui.input.SingleInputView
import com.sfag.main.config.JFF_OPEN_MIME_TYPES
import com.sfag.main.config.JFF_SAVE_MIME_TYPE
import com.sfag.main.ui.component.ExportFile
import com.sfag.main.ui.component.ImportFile
import kotlinx.coroutines.launch

private enum class GrammarMode {
    GRAMMAR_EDITOR,
    SINGLE_INPUT,
    MULTI_INPUT,
}

@Composable
fun GrammarScreen(navBack: () -> Unit, snackbarHostState: SnackbarHostState) {
    val grammarViewModel: GrammarViewModel = hiltViewModel()
    val inputsViewModel: MultiInputViewModel = hiltViewModel()
    val context = LocalContext.current
    val currentMode = remember { mutableStateOf(GrammarMode.GRAMMAR_EDITOR) }
    val selectedInput = remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val importErrorMsg = stringResource(R.string.file_import_error)
    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
            uri: Uri? ->
            uri?.let {
                try {
                    grammarViewModel.loadFromJffUri(context, it)
                } catch (e: Exception) {
                    Log.e("GrammarScreen", "Failed to import file", e)
                    scope.launch { snackbarHostState.showSnackbar(importErrorMsg) }
                }
            }
        }
    val exportErrorMsg = stringResource(R.string.file_export_error)
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(JFF_SAVE_MIME_TYPE)
        ) { uri: Uri? ->
            try {
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(
                            grammarViewModel
                                .getIndividualRules()
                                .exportToJff()
                                .toByteArray(Charsets.UTF_8)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GrammarScreen", "Failed to export file", e)
                scope.launch { snackbarHostState.showSnackbar(exportErrorMsg) }
            }
        }
    BackHandler {
        when (currentMode.value) {
            GrammarMode.GRAMMAR_EDITOR -> navBack()
            GrammarMode.SINGLE_INPUT,
            GrammarMode.MULTI_INPUT -> currentMode.value = GrammarMode.GRAMMAR_EDITOR
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        GrammarTopBar(
            currentMode = currentMode,
            grammarViewModel = grammarViewModel,
            onImport = { importLauncher.launch(JFF_OPEN_MIME_TYPES) },
            onExport = { exportLauncher.launch("grammar.jff") },
        )
        when (currentMode.value) {
            GrammarMode.GRAMMAR_EDITOR ->
                GrammarEditor(
                    grammarViewModel,
                    snackbarHostState,
                    modifier = Modifier.fillMaxSize(),
                )

            GrammarMode.SINGLE_INPUT ->
                SingleInputView(
                    grammarViewModel,
                    initialInput = selectedInput.value,
                    modifier = Modifier.fillMaxSize(),
                )

            GrammarMode.MULTI_INPUT ->
                MultiInputView(
                    grammarViewModel,
                    inputsViewModel,
                    onTestInput = { input ->
                        selectedInput.value = input
                        currentMode.value = GrammarMode.SINGLE_INPUT
                    },
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

private data class NavAction(val dest: GrammarMode, val icon: ImageVector, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrammarTopBar(
    currentMode: MutableState<GrammarMode>,
    grammarViewModel: GrammarViewModel,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {
            Row {
                IconButton(onClick = onExport, modifier = Modifier.size(48.dp)) {
                    Icon(
                        ExportFile,
                        contentDescription = stringResource(R.string.export_grammar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onImport, modifier = Modifier.size(48.dp)) {
                    Icon(
                        ImportFile,
                        contentDescription = stringResource(R.string.import_grammar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            val navActions =
                listOf(
                    NavAction(
                        GrammarMode.GRAMMAR_EDITOR,
                        Icons.Default.Build,
                        stringResource(R.string.grammar_editor),
                    ),
                    NavAction(
                        GrammarMode.SINGLE_INPUT,
                        Icons.Default.PlayArrow,
                        stringResource(R.string.single_input),
                    ),
                    NavAction(
                        GrammarMode.MULTI_INPUT,
                        Icons.AutoMirrored.Filled.List,
                        stringResource(R.string.multi_input),
                    ),
                )
            navActions.forEach { navItem ->
                IconButton(
                    onClick = {
                        if (
                            currentMode.value != navItem.dest && grammarViewModel.isGrammarFinished
                        ) {
                            currentMode.value = navItem.dest
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label,
                        tint =
                            if (currentMode.value == navItem.dest) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        },
    )
}
