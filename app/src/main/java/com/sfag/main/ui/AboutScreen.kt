package com.sfag.main.ui

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
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
import java.util.Locale

@Composable
fun AboutScreen(navBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .verticalScroll(scrollState)
                .padding(16.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_wide),
            contentDescription = "Logo",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_automata_features),
            style = MaterialTheme.typography.titleMedium,
        )
        FeatureList(
            listOf(
                stringResource(R.string.feature_edit_automata),
                stringResource(R.string.feature_determinism),
                stringResource(R.string.feature_formal_definition),
                stringResource(R.string.feature_visualize_derivation),
                stringResource(R.string.feature_multi_input_short),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_grammar_features),
            style = MaterialTheme.typography.titleMedium,
        )
        FeatureList(
            listOf(
                stringResource(R.string.feature_edit_grammar),
                stringResource(R.string.feature_grammar_type),
                stringResource(R.string.feature_formal_grammar),
                stringResource(R.string.feature_visualize_grammar),
                stringResource(R.string.feature_multi_grammar),
            ),
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.authors_label), style = MaterialTheme.typography.titleMedium)
        Text("Peter Chovanec (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Vadim Rohach (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Jakub Taňkoš (2026)", style = MaterialTheme.typography.bodyMedium)
        Text("Juraj Lopušek (2026)", style = MaterialTheme.typography.bodyMedium)
        Text("Martin Lukačka (2026)", style = MaterialTheme.typography.bodyMedium)
        Text("Samuel Strečko (2026)", style = MaterialTheme.typography.bodyMedium)
        Text("Slavomír Tung Le Minh (2026)", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.supervisor_label), style = MaterialTheme.typography.titleMedium)
        Text("doc. Mgr. Daniela Chudá, PhD.", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        LanguageSelector()

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.university_label), style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.version_label), style = MaterialTheme.typography.labelSmall)
    }

    BackHandler { navBack() }
}

@Composable
private fun LanguageSelector() {
    val currentLanguage =
        AppCompatDelegate.getApplicationLocales().get(0)?.language
            ?: Locale.getDefault().language

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageButton(
                text = stringResource(R.string.language_slovak),
                isSelected = currentLanguage == "sk",
                modifier = Modifier.weight(1f)
            ) {
                changeLanguage("sk")
            }
            LanguageButton(
                text = stringResource(R.string.language_english),
                isSelected = currentLanguage == "en",
                modifier = Modifier.weight(1f)
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
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun changeLanguage(languageCode: String) {
    val appLocale = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
}

@Composable
private fun FeatureList(items: List<String>) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { Text(text = "• $it", style = MaterialTheme.typography.bodySmall) }
    }
}
