package com.aiassistant.presentation.recurring_task_list

import com.aiassistant.domain.model.RecurringTask

data class RecurringTaskListState(
    val tasks: List<RecurringTask> = emptyList(),
    val isLoading: Boolean = true
)
