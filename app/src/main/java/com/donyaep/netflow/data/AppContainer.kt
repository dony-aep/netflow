package com.donyaep.netflow.data

import android.content.Context
import androidx.room.Room
import com.donyaep.netflow.data.local.NetFlowDatabase
import com.donyaep.netflow.data.local.SettingsLocalDataSource
import com.donyaep.netflow.data.local.TrafficStatsLocalDataSource
import com.donyaep.netflow.data.repository.DailyUsageRepository
import com.donyaep.netflow.data.repository.DefaultDailyUsageRepository
import com.donyaep.netflow.data.repository.DefaultSettingsRepository
import com.donyaep.netflow.data.repository.DefaultTrafficStatsRepository
import com.donyaep.netflow.data.repository.SettingsRepository
import com.donyaep.netflow.data.repository.TrafficStatsRepository

interface AppContainer {
    val trafficStatsRepository: TrafficStatsRepository
    val dailyUsageRepository: DailyUsageRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    private val database: NetFlowDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            NetFlowDatabase::class.java,
            "netflow.db",
        ).build()
    }

    override val trafficStatsRepository: TrafficStatsRepository by lazy {
        DefaultTrafficStatsRepository(
            localDataSource = TrafficStatsLocalDataSource(),
        )
    }

    override val dailyUsageRepository: DailyUsageRepository by lazy {
        DefaultDailyUsageRepository(database.dailyUsageDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        DefaultSettingsRepository(SettingsLocalDataSource(appContext))
    }
}