package com.sfag.grammar.presentation.navigation

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector


data class DestinationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

object DestinationConstants {
    val DestinationItems = listOf(
        DestinationItem("Grammar", Icons.Default.Build, "grammarScreen"),
        DestinationItem("Test", Icons.Default.PlayArrow, "testScreen"),
        DestinationItem("Bulk Test", Icons.AutoMirrored.Filled.List, "bulkTestScreen")
    )
}
