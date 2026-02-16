package com.aiassistant.domain.model

data class RecurringTask(
    val id: Long = 0,
    val title: String,
    val prompt: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<Int>,
    val scheduleDisplay: String,
    val relatedApps: List<String>,
    val isTimeSensitive: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long,
    val lastRunAt: Long? = null,
    val lastRunStatus: String? = null,
    val lastRunSummary: String? = null
)

data class TaskExecution(
    val id: Long = 0,
    val taskId: Long,
    val executedAt: Long,
    val status: String,
    val summary: String?,
    val durationMs: Long?
)
