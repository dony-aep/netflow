package com.donyaep.netflow.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.donyaep.netflow.ui.navigation.AppNavHost

@Composable
fun NetFlowApp() {
    val navController = rememberNavController()

    // Aporta el fondo opaco y los márgenes de las barras del sistema. Cada pantalla trae su
    // propia barra superior, para que se deslice con ella al navegar.
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        AppNavHost(
            navController = navController,
            contentPadding = innerPadding,
        )
    }
}
