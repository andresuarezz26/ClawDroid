package com.aiassistant.data.local.entity.recurringtask

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_executions",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class TaskExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val executedAt: Long,
    val status: String,
    val summary: String?,
    val durationMs: Long?
)
