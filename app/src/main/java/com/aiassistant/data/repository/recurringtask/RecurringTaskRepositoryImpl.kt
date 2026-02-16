package com.aiassistant.data.repository.recurringtask

import com.aiassistant.data.local.dao.recurringtask.RecurringTaskDao
import com.aiassistant.data.local.entity.recurringtask.RecurringTaskEntity
import com.aiassistant.data.local.entity.recurringtask.TaskExecutionEntity
import com.aiassistant.domain.model.RecurringTask
import com.aiassistant.domain.model.TaskExecution
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTaskRepositoryImpl @Inject constructor(
    private val dao: RecurringTaskDao
) : RecurringTaskRepository {

    override fun observeAll(): Flow<List<RecurringTask>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getAll(): List<RecurringTask> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun getById(id: Long): RecurringTask? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getEnabled(): List<RecurringTask> {
        return dao.getEnabled().map { it.toDomain() }
    }

    override suspend fun create(task: RecurringTask): Long {
        return dao.insert(task.toEntity())
    }

    override suspend fun update(task: RecurringTask) {
        dao.update(task.toEntity())
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun recordExecution(execution: TaskExecution) {
        dao.insertExecution(execution.toEntity())
    }

    override suspend fun getRecentExecutions(taskId: Long, limit: Int): List<TaskExecution> {
        return dao.getRecentExecutions(taskId, limit).map { it.toDomain() }
    }

    private fun RecurringTaskEntity.toDomain() = RecurringTask(
        id = id,
        title = title,
        prompt = prompt,
        hour = hour,
        minute = minute,
        daysOfWeek = if (daysOfWeek.isBlank()) emptyList() else daysOfWeek.split(",").map { it.trim().toInt() },
        scheduleDisplay = scheduleDisplay,
        relatedApps = if (relatedApps.isBlank()) emptyList() else relatedApps.split(",").map { it.trim() },
        isTimeSensitive = isTimeSensitive,
        enabled = enabled,
        createdAt = createdAt,
        lastRunAt = lastRunAt,
        lastRunStatus = lastRunStatus,
        lastRunSummary = lastRunSummary
    )

    private fun RecurringTask.toEntity() = RecurringTaskEntity(
        id = id,
        title = title,
        prompt = prompt,
        hour = hour,
        minute = minute,
        daysOfWeek = daysOfWeek.joinToString(","),
        scheduleDisplay = scheduleDisplay,
        relatedApps = relatedApps.joinToString(","),
        isTimeSensitive = isTimeSensitive,
        enabled = enabled,
        createdAt = createdAt,
        lastRunAt = lastRunAt,
        lastRunStatus = lastRunStatus,
        lastRunSummary = lastRunSummary
    )

    private fun TaskExecutionEntity.toDomain() = TaskExecution(
        id = id,
        taskId = taskId,
        executedAt = executedAt,
        status = status,
        summary = summary,
        durationMs = durationMs
    )

    private fun TaskExecution.toEntity() = TaskExecutionEntity(
        id = id,
        taskId = taskId,
        executedAt = executedAt,
        status = status,
        summary = summary,
        durationMs = durationMs
    )
}
