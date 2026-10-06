package com.donyaep.netflow.data.repository

import com.donyaep.netflow.data.local.DailyUsageDao
import com.donyaep.netflow.data.local.toEntity
import com.donyaep.netflow.data.local.toExternalModel
import com.donyaep.netflow.data.model.DailyUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

interface DailyUsageRepository {
    fun observeTodayUsage(): Flow<DailyUsage>
    /** Último año de consumo; `null` solo antes de la primera lectura. */
    val recentUsage: StateFlow<List<DailyUsage>?>
    suspend fun getTodayUsage(): DailyUsage
    suspend fun recordUsage(
        wifiReceivedDelta: Long,
        wifiSentDelta: Long,
        mobileReceivedDelta: Long,
        mobileSentDelta: Long,
    )

    suspend fun resetTodayUsage()
    suspend fun getTotalBytesBetween(startDate: String, endDate: String): Long
    suspend fun getMobileBytesBetween(startDate: String, endDate: String): Long
}

class DefaultDailyUsageRepository(
    private val dailyUsageDao: DailyUsageDao,
) : DailyUsageRepository {
    override fun observeTodayUsage(): Flow<DailyUsage> =
        dailyUsageDao.observeByDate(todayDate()).map { entity ->
            entity?.toExternalModel() ?: DailyUsage.empty(todayDate())
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val recentUsage: StateFlow<List<DailyUsage>?> =
        dailyUsageDao.observeRecent(RECENT_DAYS)
            .map { entities -> entities.map { it.toExternalModel() } }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Una lectura al arrancar deja el historial en memoria para la primera vez que se
        // abre. No se mantiene viva: el servicio escribe cada dos segundos y cada escritura
        // repetiría la consulta.
        scope.launch { recentUsage.filterNotNull().first() }
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

    override suspend fun getMobileBytesBetween(startDate: String, endDate: String): Long =
        dailyUsageDao.sumMobileBytesBetween(startDate, endDate)

    private fun todayDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    private companion object {
        const val RECENT_DAYS = 365
    }
}