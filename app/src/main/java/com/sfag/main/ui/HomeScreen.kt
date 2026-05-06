package com.sfag.main.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.sfag.R
import com.sfag.main.ui.component.DefaultButton
import com.sfag.main.ui.component.PillToggle
import java.util.Locale

@Composable
fun HomeScreen(
    navToAutomata: () -> Unit,
    navToGrammar: () -> Unit,
    navToExamples: () -> Unit,
    navToAbout: () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_square),
                modifier = Modifier.size(280.dp),
                contentDescription = "",
            )

            Spacer(Modifier.height(28.dp))

            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                DefaultButton(
                    onClick = { navToAutomata() },
                    text = stringResource(R.string.automata_simulator),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                DefaultButton(
                    onClick = { navToGrammar() },
                    text = stringResource(R.string.grammar_simulator),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                DefaultButton(
                    onClick = { navToExamples() },
                    text = stringResource(R.string.example_page),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                DefaultButton(
                    onClick = { navToAbout() },
                    text = stringResource(R.string.about_page),
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
            }
        }

        LanguageToggle(modifier = Modifier.align(Alignment.TopEnd).padding(28.dp))
    }
}

@Composable
private fun LanguageToggle(modifier: Modifier = Modifier) {
    val current =
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
    val options = listOf("en", "sk")
    val selectedIndex = options.indexOf(current).coerceAtLeast(0)

    PillToggle(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = { changeLanguage(options[it]) },
        modifier = modifier,
    )
}

private fun changeLanguage(languageCode: String) {
    val current =
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
    if (current == languageCode) {
        return
    }
    val appLocale = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
}
