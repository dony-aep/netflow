package com.donyaep.netflow.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.donyaep.netflow.ui.navigation.AppDestination
import com.donyaep.netflow.ui.navigation.AppNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetFlowApp() {
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route
    val isHomeScreen     = currentRoute == null || currentRoute == AppDestination.Home.route
    // La pantalla de Ajustes gestiona su propio AppBar colapsable
    val isSettingsMain   = currentRoute == AppDestination.Settings.route
    val isHistoryMain    = currentRoute == AppDestination.History.route
    val isAboutMain      = currentRoute == AppDestination.About.route
    val isAdvancedMain   = currentRoute == AppDestination.AdvancedSettings.route
    val isUpdatesMain    = currentRoute == AppDestination.Updates.route
    val hideOuterTopBar  = isSettingsMain || isHistoryMain || isAboutMain || isAdvancedMain || isUpdatesMain

    // Título del AppBar para pantallas secundarias
    val screenTitle = when (currentRoute) {
        AppDestination.History.route          -> "Historial"
        AppDestination.AdvancedSettings.route -> "Opciones avanzadas"
        AppDestination.About.route            -> "Acerca de"
        AppDestination.Updates.route          -> "Actualizaciones"
        else                                  -> ""
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!hideOuterTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (isHomeScreen) "NetFlow" else screenTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        if (!isHomeScreen) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Volver",
                                )
                            }
                        }
                    },
                    actions = {
                        if (isHomeScreen) {
                            IconButton(
                                onClick = {
                                    navController.navigate(AppDestination.History.route) {
                                        launchSingleTop = true
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = "Historial")
                            }
                            IconButton(
                                onClick = {
                                    navController.navigate(AppDestination.Settings.route) {
                                        launchSingleTop = true
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Ajustes")
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            contentPadding = innerPadding,
        )
    }
}
