package com.donyaep.netflow.ui.navigation

sealed class AppDestination(
    val route: String,
    val label: String,
) {
    data object Home : AppDestination(
        route = "home",
        label = "Inicio",
    )

    data object History : AppDestination(
        route = "history",
        label = "Historial",
    )

    data object Settings : AppDestination(
        route = "settings",
        label = "Ajustes",
    )

    data object AdvancedSettings : AppDestination(
        route = "settings/advanced",
        label = "Avanzado",
    )

    data object About : AppDestination(
        route = "settings/about",
        label = "Acerca de",
    )

    data object Updates : AppDestination(
        route = "settings/updates",
        label = "Actualizaciones",
    )

    companion object {
        val topLevel = listOf(Home, History, Settings)
    }
}