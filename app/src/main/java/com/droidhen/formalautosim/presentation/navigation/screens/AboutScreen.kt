package com.droidhen.formalautosim.presentation.navigation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.droidhen.formalautosim.R
import views.FASButton

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
        Text("Vedúca: doc. Mgr. Daniela Chudá, PhD.", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("FEI STU Bratislava", style = MaterialTheme.typography.labelMedium)
        Text("AutoGram simulator v1.3 ©2025", style = MaterialTheme.typography.labelSmall)
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            FASButton(
                modifier = Modifier.padding(16.dp),
                text = "BACK") {
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
