package com.donyaep.netflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.ui.NetFlowApp
import com.donyaep.netflow.ui.theme.NetFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContainer = (application as NetFlowApplication).appContainer
            val settings = appContainer.settingsRepository
                .observeSettings()
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            NetFlowTheme(themeMode = settings.value.themeMode) {
                NetFlowApp()
            }
        }
    }
}