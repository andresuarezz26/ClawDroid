package com.aiassistant.data.local.entity.recurringtask

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_tasks")
data class RecurringTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val prompt: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String,
    val scheduleDisplay: String,
    val relatedApps: String,
    val isTimeSensitive: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long,
    val lastRunAt: Long? = null,
    val lastRunStatus: String? = null,
    val lastRunSummary: String? = null
)
