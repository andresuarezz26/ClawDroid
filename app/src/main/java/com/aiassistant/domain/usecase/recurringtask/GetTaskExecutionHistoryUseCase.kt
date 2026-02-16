package com.aiassistant.domain.usecase.recurringtask

import com.aiassistant.domain.model.TaskExecution
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import javax.inject.Inject

class GetTaskExecutionHistoryUseCase @Inject constructor(
    private val repository: RecurringTaskRepository
) {
    suspend operator fun invoke(taskId: Long, limit: Int = 10): List<TaskExecution> {
        return repository.getRecentExecutions(taskId, limit)
    }
}
