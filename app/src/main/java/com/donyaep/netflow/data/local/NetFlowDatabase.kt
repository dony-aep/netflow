package com.donyaep.netflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyUsageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NetFlowDatabase : RoomDatabase() {
    abstract fun dailyUsageDao(): DailyUsageDao
}