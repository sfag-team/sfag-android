package com.sfag.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.sfag.R

@Composable
fun AboutScreen(navBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .verticalScroll(scrollState)
        .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_wide),
            contentDescription = "Logo"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AutoGram simulátor je aplikácia umožňujúca pochopiť prácu s formálnymi jazykmi Chomského hierarchie jazykov. V aplikácii možno pracovať so simulátorom konečných a zásobníkových automatov a simulátorom gramatík.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Simulátor umožňuje pre konečný/zásobníkový automat:",
            style = MaterialTheme.typography.titleMedium
        )
        FeatureList(
            listOf(
                "editovať stavový diagram automatu, stavy, prechody",
                "určí či je automat deterministický alebo nedeterministický",
                "sumarizuje formálny zápis automatu",
                "vizualizuje odvodenie vstupného slova v diagrame",
                "zhodnotiť akceptovanie viacerých slov"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Simulátor umožňuje pre gramatiky:",
            style = MaterialTheme.typography.titleMedium
        )
        FeatureList(
            listOf(
                "editovať pravidlá gramatiky",
                "určí typ gramatiky",
                "sumarizuje formálny zápis gramatiky",
                "umožňuje vizualizovať odvodenie vstupného slova",
                "zhodnotiť či viacero vstupných slov bolo odvodených"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Autori:", style = MaterialTheme.typography.titleMedium)
        Text("Peter Chovanec (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Vadim Rohach (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Jakub Taňkoš (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Juraj Lopušek (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Martin Lukačka (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Samuel Strečko (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Slavomír Tung Le Minh (2025)", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Vedúca:", style = MaterialTheme.typography.titleMedium)
        Text("doc. Mgr. Daniela Chudá, PhD.", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("FEI STU Bratislava", style = MaterialTheme.typography.labelMedium)
        Text("AutoGram simulator v2.1 ©2025", style = MaterialTheme.typography.labelSmall)
    }

    BackHandler {
        navBack()
    }
}

@Composable
private fun FeatureList(items: List<String>) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach {
            Text(text = "• $it", style = MaterialTheme.typography.bodySmall)
        }
    }
}
