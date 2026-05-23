package com.donyaep.netflow.data.local

import android.net.TrafficStats
import com.donyaep.netflow.data.model.TrafficSnapshot

class TrafficStatsLocalDataSource {
    fun readSnapshot(): TrafficSnapshot {
        val totalRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        val totalTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
        val mobileRx = TrafficStats.getMobileRxBytes().coerceAtLeast(0L)
        val mobileTx = TrafficStats.getMobileTxBytes().coerceAtLeast(0L)

        return TrafficSnapshot(
            totalRxBytes = totalRx,
            totalTxBytes = totalTx,
            mobileRxBytes = mobileRx,
            mobileTxBytes = mobileTx,
        )
    }
}
