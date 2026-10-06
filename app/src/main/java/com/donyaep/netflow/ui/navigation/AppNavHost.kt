package com.donyaep.netflow.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
    val motion = MaterialTheme.motionScheme

    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = Modifier.padding(contentPadding),
        // La pantalla nueva entra opaca por encima de la anterior y ninguna se desvanece. Con
        // el fundido por defecto las dos quedan semitransparentes a la vez y el cruce se ve
        // como un destello.
        enterTransition = { slideInHorizontally(motion.defaultSpatialSpec()) { it } },
        exitTransition = { slideOutHorizontally(motion.defaultSpatialSpec()) { -it / 4 } },
        // Al volver se invierte: la que se va sale por la derecha, encima de la que vuelve.
        popEnterTransition = { slideInHorizontally(motion.defaultSpatialSpec()) { -it / 4 } },
        popExitTransition = { slideOutHorizontally(motion.defaultSpatialSpec()) { it } },
    ) {
        composable(AppDestination.Home.route) {
            HomeRoute(
                onOpenHistory = {
                    navController.navigate(AppDestination.History.route) { launchSingleTop = true }
                },
                onOpenSettings = {
                    navController.navigate(AppDestination.Settings.route) { launchSingleTop = true }
                },
            )
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
