package com.donyaep.netflow.ui.screens.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donyaep.netflow.BuildConfig
import com.donyaep.netflow.core.update.GitHubUpdateService
import com.donyaep.netflow.core.update.ReleaseInfo
import com.donyaep.netflow.core.update.UpdateCheckStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val isChecking: Boolean = false,
    val status: UpdateCheckStatus? = null,
    val releaseInfo: ReleaseInfo? = null,
    val message: String? = null,
)

class UpdateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, message = null)
            val result = GitHubUpdateService.checkForUpdates(_uiState.value.currentVersion)
            _uiState.value = _uiState.value.copy(
                isChecking  = false,
                status      = result.status,
                releaseInfo = result.releaseInfo,
                message     = result.message,
            )
        }
    }
}
