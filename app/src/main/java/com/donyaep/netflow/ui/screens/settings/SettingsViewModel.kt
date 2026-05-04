package com.donyaep.netflow.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.donyaep.netflow.NetFlowApplication
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.data.model.DataLimitUnit
import com.donyaep.netflow.data.model.SpeedUnit
import com.donyaep.netflow.data.model.ThemeMode
import com.donyaep.netflow.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SettingsRepository = (application as NetFlowApplication).appContainer.settingsRepository

    private val _uiState = MutableStateFlow(AppSettings())
    val uiState: StateFlow<AppSettings> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.value = settings
            }
        }
    }

    fun cycleThemeMode() {
        viewModelScope.launch {
            val next = when (_uiState.value.themeMode) {
                ThemeMode.System -> ThemeMode.Light
                ThemeMode.Light -> ThemeMode.Dark
                ThemeMode.Dark -> ThemeMode.System
            }
            repository.updateThemeMode(next)
        }
    }

    fun cycleSpeedUnit() {
        viewModelScope.launch {
            val next = when (_uiState.value.speedUnit) {
                SpeedUnit.BitsPerSecond -> SpeedUnit.BytesPerSecond
                SpeedUnit.BytesPerSecond -> SpeedUnit.BitsPerSecond
            }
            repository.updateSpeedUnit(next)
        }
    }

    fun toggleHideOnLockscreen() {
        viewModelScope.launch {
            repository.updateHideOnLockscreen(!_uiState.value.hideOnLockscreen)
        }
    }

    fun toggleDataLimitEnabled() {
        viewModelScope.launch {
            repository.updateDataLimitEnabled(!_uiState.value.dataLimitEnabled)
        }
    }

    fun incrementBillingCycleDay() {
        viewModelScope.launch {
            repository.updateBillingCycleDay((_uiState.value.billingCycleDay % 28) + 1)
        }
    }

    fun cycleDataLimitUnit() {
        viewModelScope.launch {
            val next = when (_uiState.value.dataLimitUnit) {
                DataLimitUnit.KB -> DataLimitUnit.MB
                DataLimitUnit.MB -> DataLimitUnit.GB
                DataLimitUnit.GB -> DataLimitUnit.KB
            }
            repository.updateDataLimitUnit(next)
        }
    }

    fun adjustDataLimit(delta: Double) {
        viewModelScope.launch {
            val nextValue = (_uiState.value.dataLimitValue + delta).coerceAtLeast(0.5)
            repository.updateDataLimitValue(nextValue)
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { repository.updateThemeMode(themeMode) }
    }

    fun setSpeedUnit(speedUnit: SpeedUnit) {
        viewModelScope.launch { repository.updateSpeedUnit(speedUnit) }
    }

    fun setDataLimit(value: Double, unit: DataLimitUnit, billingDay: Int) {
        viewModelScope.launch {
            repository.updateDataLimitValue(value)
            repository.updateDataLimitUnit(unit)
            repository.updateBillingCycleDay(billingDay)
        }
    }
}