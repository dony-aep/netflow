package com.donyaep.netflow.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.data.model.DataLimitUnit
import com.donyaep.netflow.data.model.SpeedUnit
import com.donyaep.netflow.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class SettingsLocalDataSource(context: Context) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(context.filesDir, "datastore/netflow_settings.preferences_pb") },
    )

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[THEME_MODE]?.toEnumOrDefault(ThemeMode.System) ?: ThemeMode.System,
            speedUnit = preferences[SPEED_UNIT]?.toEnumOrDefault(SpeedUnit.BytesPerSecond) ?: SpeedUnit.BytesPerSecond,
            hideOnLockscreen = preferences[HIDE_ON_LOCKSCREEN] ?: false,
            restoreMonitoringAfterBoot = preferences[RESTORE_MONITORING_AFTER_BOOT] ?: false,
            dataLimitEnabled = preferences[DATA_LIMIT_ENABLED] ?: false,
            dataLimitValue = preferences[DATA_LIMIT_VALUE] ?: 5.0,
            dataLimitUnit = preferences[DATA_LIMIT_UNIT]?.toEnumOrDefault(DataLimitUnit.GB) ?: DataLimitUnit.GB,
            billingCycleDay = preferences[BILLING_CYCLE_DAY] ?: 1,
        )
    }

    suspend fun getSettings(): AppSettings = settings.first()

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateSpeedUnit(speedUnit: SpeedUnit) {
        dataStore.edit { preferences ->
            preferences[SPEED_UNIT] = speedUnit.name
        }
    }

    suspend fun updateHideOnLockscreen(hideOnLockscreen: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIDE_ON_LOCKSCREEN] = hideOnLockscreen
        }
    }

    suspend fun updateRestoreMonitoringAfterBoot(restoreAfterBoot: Boolean) {
        dataStore.edit { preferences ->
            preferences[RESTORE_MONITORING_AFTER_BOOT] = restoreAfterBoot
        }
    }

    suspend fun updateDataLimitEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DATA_LIMIT_ENABLED] = enabled
        }
    }

    suspend fun updateDataLimitValue(dataLimitValue: Double) {
        dataStore.edit { preferences ->
            preferences[DATA_LIMIT_VALUE] = dataLimitValue
        }
    }

    suspend fun updateDataLimitUnit(dataLimitUnit: DataLimitUnit) {
        dataStore.edit { preferences ->
            preferences[DATA_LIMIT_UNIT] = dataLimitUnit.name
        }
    }

    suspend fun updateBillingCycleDay(day: Int) {
        dataStore.edit { preferences ->
            preferences[BILLING_CYCLE_DAY] = day.coerceIn(1, 31)
        }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(defaultValue: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: defaultValue

    private companion object {
        val THEME_MODE = stringPreferencesKey("themeMode")
        val SPEED_UNIT = stringPreferencesKey("speedUnit")
        val HIDE_ON_LOCKSCREEN = booleanPreferencesKey("hideOnLockscreen")
        val RESTORE_MONITORING_AFTER_BOOT = booleanPreferencesKey("restoreMonitoringAfterBoot")
        val DATA_LIMIT_ENABLED = booleanPreferencesKey("dataLimitEnabled")
        val DATA_LIMIT_VALUE = doublePreferencesKey("dataLimitValue")
        val DATA_LIMIT_UNIT = stringPreferencesKey("dataLimitUnit")
        val BILLING_CYCLE_DAY = intPreferencesKey("billingCycleDay")
    }
}