package com.donyaep.netflow.data.repository

import com.donyaep.netflow.data.local.DailyUsageDao
import com.donyaep.netflow.data.local.toEntity
import com.donyaep.netflow.data.local.toExternalModel
import com.donyaep.netflow.data.model.DailyUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

interface DailyUsageRepository {
    fun observeTodayUsage(): Flow<DailyUsage>
    fun observeRecentUsage(limit: Int): Flow<List<DailyUsage>>
    suspend fun getTodayUsage(): DailyUsage
    suspend fun recordUsage(
        wifiReceivedDelta: Long,
        wifiSentDelta: Long,
        mobileReceivedDelta: Long,
        mobileSentDelta: Long,
    )

    suspend fun resetTodayUsage()
    suspend fun getTotalBytesBetween(startDate: String, endDate: String): Long
}

class DefaultDailyUsageRepository(
    private val dailyUsageDao: DailyUsageDao,
) : DailyUsageRepository {
    override fun observeTodayUsage(): Flow<DailyUsage> =
        dailyUsageDao.observeByDate(todayDate()).map { entity ->
            entity?.toExternalModel() ?: DailyUsage.empty(todayDate())
        }

    override fun observeRecentUsage(limit: Int): Flow<List<DailyUsage>> =
        dailyUsageDao.observeRecent(limit).map { entities ->
            entities.map { it.toExternalModel() }
        }

    override suspend fun getTodayUsage(): DailyUsage =
        dailyUsageDao.getByDate(todayDate())?.toExternalModel() ?: DailyUsage.empty(todayDate())

    override suspend fun recordUsage(
        wifiReceivedDelta: Long,
        wifiSentDelta: Long,
        mobileReceivedDelta: Long,
        mobileSentDelta: Long,
    ) {
        if (
            wifiReceivedDelta == 0L &&
            wifiSentDelta == 0L &&
            mobileReceivedDelta == 0L &&
            mobileSentDelta == 0L
        ) {
            return
        }
        dailyUsageDao.addUsage(
            date = todayDate(),
            wifiRx = wifiReceivedDelta,
            wifiTx = wifiSentDelta,
            mobileRx = mobileReceivedDelta,
            mobileTx = mobileSentDelta,
        )
    }

    override suspend fun resetTodayUsage() {
        dailyUsageDao.deleteByDate(todayDate())
    }

    override suspend fun getTotalBytesBetween(startDate: String, endDate: String): Long =
        dailyUsageDao.sumTotalBytesBetween(startDate, endDate)

    private fun todayDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
}