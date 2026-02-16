package com.aiassistant.presentation.recurring_task_list

sealed interface RecurringTaskListIntent {
    data class ToggleTask(val taskId: Long, val enabled: Boolean) : RecurringTaskListIntent
}
