package com.donyaep.netflow.core.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object MonitoringStateStore {
    private val _state = MutableStateFlow(MonitoringState())
    val state: StateFlow<MonitoringState> = _state.asStateFlow()

    fun update(transform: (MonitoringState) -> MonitoringState) {
        _state.update(transform)
    }

    fun reset() {
        _state.update { MonitoringState() }
    }
}