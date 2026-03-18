package com.sfag.grammar

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.sfag.R
import com.sfag.grammar.data.exportToJff
import com.sfag.grammar.ui.GrammarScreen
import com.sfag.grammar.ui.GrammarViewModel
import com.sfag.grammar.ui.MultiInputScreen
import com.sfag.grammar.ui.MultiInputViewModel
import com.sfag.grammar.ui.SingleInputScreen
import com.sfag.main.config.EXTRA_EXAMPLE_URI
import com.sfag.main.config.JFF_OPEN_MIME_TYPES
import com.sfag.main.config.JFF_SAVE_MIME_TYPE
import com.sfag.main.configureScreenOrientation
import com.sfag.main.ui.component.DefaultSnackbarHost
import com.sfag.main.ui.component.ExportFile
import com.sfag.main.ui.component.ImportFile
import com.sfag.main.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

private enum class Mode {
    GRAMMAR_EDITOR,
    SINGLE_INPUT,
    MULTI_INPUT,
}

@AndroidEntryPoint
class GrammarActivity : AppCompatActivity() {
    private val viewModel: GrammarViewModel by viewModels()

    override fun onStop() {
        super.onStop()
        viewModel.autoSave()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureScreenOrientation()

        val assetPath = intent?.getStringExtra(EXTRA_EXAMPLE_URI)

        setContent {
            AppTheme {
                val inputsViewModel: MultiInputViewModel =
                    androidx.hilt.lifecycle.viewmodel.compose
                        .hiltViewModel()
                val currentMode = remember { mutableStateOf(Mode.GRAMMAR_EDITOR) }
                val selectedInput = remember { mutableStateOf<String?>(null) }
                LaunchedEffect(assetPath) {
                    if (assetPath != null) {
                        val inputStream = assets.open(assetPath)
                        viewModel.loadFromXmlStream(inputStream)
                    }
                }
                val snackbarHostState = remember { SnackbarHostState() }
                val importLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                    ) { uri: Uri? ->
                        uri?.let { viewModel.loadFromXmlUri(this@GrammarActivity, it) }
                    }
                val exportLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument(JFF_SAVE_MIME_TYPE),
                    ) { uri: Uri? ->
                        uri?.let {
                            contentResolver.openOutputStream(it)?.use { stream ->
                                stream.write(
                                    viewModel
                                        .getIndividualRules()
                                        .exportToJff()
                                        .toByteArray(Charsets.UTF_8),
                                )
                            }
                        }
                    }
                BackHandler {
                    when (currentMode.value) {
                        Mode.GRAMMAR_EDITOR -> finish()
                        Mode.SINGLE_INPUT,
                        Mode.MULTI_INPUT,
                        -> currentMode.value = Mode.GRAMMAR_EDITOR
                    }
                }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    topBar = {
                        TopNavigationBar(
                            currentMode = currentMode,
                            grammarViewModel = viewModel,
                            onImport = { importLauncher.launch(JFF_OPEN_MIME_TYPES) },
                            onExport = { exportLauncher.launch("grammar.jff") },
                        )
                    },
                    snackbarHost = { DefaultSnackbarHost(snackbarHostState) },
                ) { padding ->
                    when (currentMode.value) {
                        Mode.GRAMMAR_EDITOR ->
                            GrammarScreen(
                                viewModel,
                                snackbarHostState,
                                modifier = Modifier.padding(padding),
                            )
                        Mode.SINGLE_INPUT ->
                            SingleInputScreen(
                                viewModel,
                                initialInput = selectedInput.value,
                                modifier = Modifier.padding(padding),
                            )
                        Mode.MULTI_INPUT ->
                            MultiInputScreen(
                                viewModel,
                                inputsViewModel,
                                onTestInput = { input ->
                                    selectedInput.value = input
                                    currentMode.value = Mode.SINGLE_INPUT
                                },
                                modifier = Modifier.padding(padding),
                            )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopNavigationBar(
    currentMode: MutableState<Mode>,
    grammarViewModel: GrammarViewModel,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {
            Row {
                IconButton(onClick = onExport) {
                    Icon(
                        ExportFile,
                        contentDescription = stringResource(R.string.export_grammar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onImport) {
                    Icon(
                        ImportFile,
                        contentDescription = stringResource(R.string.import_grammar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            data class NavAction(
                val dest: Mode,
                val icon: ImageVector,
                val label: String,
            )
            val navActions =
                listOf(
                    NavAction(Mode.GRAMMAR_EDITOR, Icons.Default.Build, stringResource(R.string.grammar_editor)),
                    NavAction(Mode.SINGLE_INPUT, Icons.Default.PlayArrow, stringResource(R.string.single_input)),
                    NavAction(Mode.MULTI_INPUT, Icons.AutoMirrored.Filled.List, stringResource(R.string.multi_input)),
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
