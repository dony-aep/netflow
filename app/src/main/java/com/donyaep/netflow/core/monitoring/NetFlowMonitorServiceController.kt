package com.donyaep.netflow.core.monitoring

import android.content.Context
import android.content.Intent
import android.os.Build
import com.donyaep.netflow.NetFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NetFlowMonitorServiceController {
    fun start(context: Context) {
        persistRestoreAfterBoot(context, true)
        val intent = Intent(context, NetFlowMonitorService::class.java).apply {
            action = NetFlowMonitorService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        persistRestoreAfterBoot(context, false)
        val intent = Intent(context, NetFlowMonitorService::class.java).apply {
            action = NetFlowMonitorService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun resetToday(context: Context) {
        val intent = Intent(context, NetFlowMonitorService::class.java).apply {
            action = NetFlowMonitorService.ACTION_RESET_TODAY
        }
        context.startService(intent)
    }

    private fun persistRestoreAfterBoot(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val settingsRepository = (appContext as NetFlowApplication).appContainer.settingsRepository
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.updateRestoreMonitoringAfterBoot(enabled)
        }
    }
}