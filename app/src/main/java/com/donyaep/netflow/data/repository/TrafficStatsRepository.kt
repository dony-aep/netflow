package com.donyaep.netflow.data.repository

import com.donyaep.netflow.data.local.TrafficStatsLocalDataSource
import com.donyaep.netflow.data.model.TrafficSnapshot

interface TrafficStatsRepository {
    fun readSnapshot(): TrafficSnapshot
}

class DefaultTrafficStatsRepository(
    private val localDataSource: TrafficStatsLocalDataSource,
) : TrafficStatsRepository {
    override fun readSnapshot(): TrafficSnapshot = localDataSource.readSnapshot()
}