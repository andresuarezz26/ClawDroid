package com.aiassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val resultType: String,
    val resultSummary: String,
    val stepsUsed: Int,
    val timestamp: Long
)