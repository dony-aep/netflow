package com.donyaep.netflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Query("SELECT * FROM daily_usage WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DailyUsageEntity?>

    @Query("SELECT * FROM daily_usage WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyUsageEntity>>

    @Upsert
    suspend fun upsert(entity: DailyUsageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyUsageEntity)

    @Query("""
        UPDATE daily_usage
        SET wifiReceived    = wifiReceived    + :wifiRx,
            wifiSent        = wifiSent        + :wifiTx,
            mobileReceived  = mobileReceived  + :mobileRx,
            mobileSent      = mobileSent      + :mobileTx
        WHERE date = :date
    """)
    suspend fun incrementUsage(
        date: String,
        wifiRx: Long,
        wifiTx: Long,
        mobileRx: Long,
        mobileTx: Long,
    )

    @Transaction
    suspend fun addUsage(
        date: String,
        wifiRx: Long,
        wifiTx: Long,
        mobileRx: Long,
        mobileTx: Long,
    ) {
        insertIfAbsent(DailyUsageEntity(date = date))
        incrementUsage(date, wifiRx, wifiTx, mobileRx, mobileTx)
    }

    @Query("""
        SELECT COALESCE(SUM(wifiReceived + wifiSent + mobileReceived + mobileSent), 0)
        FROM daily_usage
        WHERE date >= :startDate AND date <= :endDate
    """)
    suspend fun sumTotalBytesBetween(startDate: String, endDate: String): Long

    @Query("DELETE FROM daily_usage WHERE date = :date")
    suspend fun deleteByDate(date: String)
}