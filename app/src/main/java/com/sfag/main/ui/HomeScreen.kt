package com.sfag.main.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.sfag.R
import com.sfag.main.ui.component.DefaultButton
import java.util.Locale

@Composable
fun HomeScreen(
    navToAutomata: () -> Unit,
    navToGrammar: () -> Unit,
    navToExamples: () -> Unit,
    navToAbout: () -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_square),
                modifier = Modifier.size(312.dp),
                contentDescription = "",
            )

            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
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

            Spacer(Modifier.height(24.dp))

            LanguageSelector()
        }
    }
}

@Composable
private fun LanguageSelector() {
    val currentLanguage =
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LanguageButton(
                text = stringResource(R.string.language_slovak),
                isSelected = currentLanguage == "sk",
                modifier = Modifier.weight(1f),
            ) {
                changeLanguage("sk")
            }
            LanguageButton(
                text = stringResource(R.string.language_english),
                isSelected = currentLanguage == "en",
                modifier = Modifier.weight(1f),
            ) {
                changeLanguage("en")
            }
        }
    }
}

@Composable
private fun LanguageButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                )
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun changeLanguage(languageCode: String) {
    val currentLanguage =
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
    if (currentLanguage == languageCode) {
        return
    }
    val appLocale = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
}
