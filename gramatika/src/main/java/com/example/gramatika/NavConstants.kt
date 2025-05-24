package com.example.gramatika

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

object NavConstants {
    val TopNavItems = listOf(
        NavItem("Grammar", Icons.Default.Build, "grammarScreen"),
        NavItem("Test", Icons.Default.PlayArrow, "testScreen"),
        NavItem("Bulk Test", Icons.AutoMirrored.Filled.List, "bulkTestScreen")
    )
}
