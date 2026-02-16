package com.aiassistant.domain.repository.recurringtask

import com.aiassistant.domain.model.RecurringTask
import com.aiassistant.domain.model.TaskExecution
import kotlinx.coroutines.flow.Flow

interface RecurringTaskRepository {
    fun observeAll(): Flow<List<RecurringTask>>
    suspend fun getAll(): List<RecurringTask>
    suspend fun getById(id: Long): RecurringTask?
    suspend fun getEnabled(): List<RecurringTask>
    suspend fun create(task: RecurringTask): Long
    suspend fun update(task: RecurringTask)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun delete(id: Long)
    suspend fun recordExecution(execution: TaskExecution)
    suspend fun getRecentExecutions(taskId: Long, limit: Int = 10): List<TaskExecution>
}
