package com.donyaep.netflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : AppDestination(
        route = "home",
        label = "Inicio",
        icon = Icons.Filled.Home,
    )

    data object History : AppDestination(
        route = "history",
        label = "Historial",
        icon = Icons.Filled.History,
    )

    data object Settings : AppDestination(
        route = "settings",
        label = "Ajustes",
        icon = Icons.Filled.Settings,
    )

    data object AdvancedSettings : AppDestination(
        route = "settings/advanced",
        label = "Avanzado",
        icon = Icons.Filled.Settings,
    )

    data object About : AppDestination(
        route = "settings/about",
        label = "Acerca de",
        icon = Icons.Filled.Settings,
    )

    data object Updates : AppDestination(
        route = "settings/updates",
        label = "Actualizaciones",
        icon = Icons.Filled.Settings,
    )

    companion object {
        val topLevel = listOf(Home, History, Settings)
    }
}