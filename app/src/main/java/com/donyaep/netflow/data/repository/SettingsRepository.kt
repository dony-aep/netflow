package com.donyaep.netflow.data.repository

import com.donyaep.netflow.data.local.SettingsLocalDataSource
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.data.model.DataLimitUnit
import com.donyaep.netflow.data.model.SpeedUnit
import com.donyaep.netflow.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateSpeedUnit(speedUnit: SpeedUnit)
    suspend fun updateHideOnLockscreen(hideOnLockscreen: Boolean)
    suspend fun updateRestoreMonitoringAfterBoot(restoreAfterBoot: Boolean)
    suspend fun updateDataLimitEnabled(enabled: Boolean)
    suspend fun updateDataLimitValue(dataLimitValue: Double)
    suspend fun updateDataLimitUnit(dataLimitUnit: DataLimitUnit)
    suspend fun updateBillingCycleDay(day: Int)
}

class DefaultSettingsRepository(
    private val localDataSource: SettingsLocalDataSource,
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = localDataSource.settings

    override suspend fun getSettings(): AppSettings = localDataSource.getSettings()

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        localDataSource.updateThemeMode(themeMode)
    }

    override suspend fun updateSpeedUnit(speedUnit: SpeedUnit) {
        localDataSource.updateSpeedUnit(speedUnit)
    }

    override suspend fun updateHideOnLockscreen(hideOnLockscreen: Boolean) {
        localDataSource.updateHideOnLockscreen(hideOnLockscreen)
    }

    override suspend fun updateRestoreMonitoringAfterBoot(restoreAfterBoot: Boolean) {
        localDataSource.updateRestoreMonitoringAfterBoot(restoreAfterBoot)
    }

    override suspend fun updateDataLimitEnabled(enabled: Boolean) {
        localDataSource.updateDataLimitEnabled(enabled)
    }

    override suspend fun updateDataLimitValue(dataLimitValue: Double) {
        localDataSource.updateDataLimitValue(dataLimitValue)
    }

    override suspend fun updateDataLimitUnit(dataLimitUnit: DataLimitUnit) {
        localDataSource.updateDataLimitUnit(dataLimitUnit)
    }

    override suspend fun updateBillingCycleDay(day: Int) {
        localDataSource.updateBillingCycleDay(day)
    }
}