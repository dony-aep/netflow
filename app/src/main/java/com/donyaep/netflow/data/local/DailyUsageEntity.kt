package com.donyaep.netflow.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.donyaep.netflow.data.model.DailyUsage

@Entity(
    tableName = "daily_usage",
    indices = [Index(value = ["date"], unique = true)],
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val wifiReceived: Long = 0,
    val wifiSent: Long = 0,
    val mobileReceived: Long = 0,
    val mobileSent: Long = 0,
)

fun DailyUsageEntity.toExternalModel(): DailyUsage = DailyUsage(
    date = date,
    wifiReceivedBytes = wifiReceived,
    wifiSentBytes = wifiSent,
    mobileReceivedBytes = mobileReceived,
    mobileSentBytes = mobileSent,
)

fun DailyUsage.toEntity(id: Long = 0): DailyUsageEntity = DailyUsageEntity(
    id = id,
    date = date,
    wifiReceived = wifiReceivedBytes,
    wifiSent = wifiSentBytes,
    mobileReceived = mobileReceivedBytes,
    mobileSent = mobileSentBytes,
)