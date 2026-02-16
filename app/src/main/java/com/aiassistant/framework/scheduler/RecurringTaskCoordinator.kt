package com.aiassistant.framework.scheduler

import android.util.Log
import com.aiassistant.domain.model.RecurringTask
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RecurringTaskCoordinator"

@Singleton
class RecurringTaskCoordinator @Inject constructor(
    private val repository: RecurringTaskRepository,
    private val scheduler: RecurringTaskScheduler
) {
    suspend fun createAndSchedule(task: RecurringTask): Long {
        val id = repository.create(task)
        val savedTask = repository.getById(id) ?: return id
        scheduler.scheduleTask(savedTask)
        Log.i(TAG, "Created and scheduled task $id (${task.title})")
        return id
    }

    suspend fun updateAndReschedule(task: RecurringTask) {
        repository.update(task)
        if (task.enabled) {
            scheduler.scheduleTask(task)
        } else {
            scheduler.cancelTask(task)
        }
        Log.i(TAG, "Updated and rescheduled task ${task.id}")
    }

    suspend fun toggleAndReschedule(taskId: Long, enabled: Boolean) {
        repository.setEnabled(taskId, enabled)
        val task = repository.getById(taskId) ?: return
        if (enabled) {
            scheduler.scheduleTask(task)
        } else {
            scheduler.cancelTask(task)
        }
        Log.i(TAG, "Toggled task $taskId to enabled=$enabled")
    }

    suspend fun deleteAndCancel(taskId: Long) {
        val task = repository.getById(taskId)
        if (task != null) {
            scheduler.cancelTask(task)
        }
        repository.delete(taskId)
        Log.i(TAG, "Deleted and cancelled task $taskId")
    }

    suspend fun rescheduleAllEnabled() {
        val tasks = repository.getEnabled()
        tasks.forEach { task ->
            scheduler.scheduleTask(task)
        }
        Log.i(TAG, "Rescheduled ${tasks.size} enabled tasks")
    }
}
