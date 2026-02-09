package com.sfag.home.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.shared.presentation.component.DefaultButton


@Composable
fun AboutScreen(navBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Logo + App Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.about_logo),
                contentDescription = "Logo"
            )
        }

        // About the app
        Text(
            text = "AutoGram simulátor je aplikácia umožňujúca pochopiť prácu s formálnymi jazykmi Chomského hierarchie jazykov. V aplikácii možno pracovať so simulátorom konečných a zásobníkových automatov a simulátorom gramatík.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Automaton Features
        Text("Simulátor umožňuje pre konečný/zásobníkový automat:", style = MaterialTheme.typography.titleMedium)
        FeatureList(
            listOf(
                "editovať stavový diagram automatu, stavy, prechody",
                "určí či je automat deterministický alebo nedeterministický",
                "sumarizuje formálny zápis automatu",
                "vizualizuje odvodenie vstupného slova v diagrame",
                "zhodnotiť akceptovanie viacerých slov"
            )
        )

        // Grammar Features
        Text("Simulátor umožňuje pre gramatiky:", style = MaterialTheme.typography.titleMedium)
        FeatureList(
            listOf(
                "editovať pravidlá gramatiky",
                "určí typ gramatiky",
                "sumarizuje formálny zápis gramatiky",
                "umožňuje vizualizovať odvodenie vstupného slova",
                "zhodnotiť či viacero vstupných slov bolo odvodených"
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Authors Section
        Text("Autori:", style = MaterialTheme.typography.titleMedium)
        Text("Peter Chovanec (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Vadim Rohach (2025)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Jakub Taňkoš (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Juraj Lopušek (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Martin Lukačka (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Samuel Strečko (2025)", style = MaterialTheme.typography.bodyMedium)
        Text("Slavomír Tung Le Minh (2025)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Vedúca: doc. Mgr. Daniela Chudá, PhD.", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("FEI STU Bratislava", style = MaterialTheme.typography.labelMedium)
        Text("AutoGram simulator v2.0 ©2025", style = MaterialTheme.typography.labelSmall)
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            DefaultButton(
                text = "BACK",
                modifier = Modifier.padding(16.dp)) {
                navBack()
            }
        }
    }
    BackHandler {
        navBack()
    }
}

@Composable
fun FeatureList(items: List<String>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        items.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}
