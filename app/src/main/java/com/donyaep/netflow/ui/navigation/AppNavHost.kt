package com.donyaep.netflow.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.donyaep.netflow.ui.screens.about.AboutScreen
import com.donyaep.netflow.ui.screens.advanced.AdvancedSettingsScreen
import com.donyaep.netflow.ui.screens.history.HistoryScreen
import com.donyaep.netflow.ui.screens.home.HomeRoute
import com.donyaep.netflow.ui.screens.settings.SettingsScreen
import com.donyaep.netflow.ui.screens.updates.UpdateScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = Modifier.padding(contentPadding),
    ) {
        composable(AppDestination.Home.route) {
            HomeRoute()
        }
        composable(AppDestination.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable(AppDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onOpenAdvanced = { navController.navigate(AppDestination.AdvancedSettings.route) },
                onOpenAbout = { navController.navigate(AppDestination.About.route) },
                onOpenUpdates = { navController.navigate(AppDestination.Updates.route) },
            )
        }
        composable(AppDestination.AdvancedSettings.route) {
            AdvancedSettingsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable(AppDestination.About.route) {
            AboutScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
        composable(AppDestination.Updates.route) {
            UpdateScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }
    }
}