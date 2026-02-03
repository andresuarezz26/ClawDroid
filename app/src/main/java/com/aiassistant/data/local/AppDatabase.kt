package com.aiassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aiassistant.data.local.dao.TaskLogDao
import com.aiassistant.data.local.entity.TaskLogEntity

@Database(
    entities = [TaskLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskLogDao(): TaskLogDao
}