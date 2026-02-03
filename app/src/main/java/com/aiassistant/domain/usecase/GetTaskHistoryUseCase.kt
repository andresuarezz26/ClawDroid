package com.aiassistant.domain.usecase

import com.aiassistant.domain.model.TaskLog
import com.aiassistant.domain.repository.TaskLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTaskHistoryUseCase @Inject constructor(
    private val taskLogRepository: TaskLogRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<TaskLog>> =
        taskLogRepository.getRecentLogs(limit)
}