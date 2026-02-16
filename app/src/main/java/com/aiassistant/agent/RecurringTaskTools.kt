package com.aiassistant.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import android.util.Log
import com.aiassistant.domain.model.RecurringTask
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import com.aiassistant.framework.scheduler.RecurringTaskCoordinator
import javax.inject.Inject

private const val TAG = "Agent"

@LLMDescription("Tools for managing recurring scheduled tasks")
class RecurringTaskTools @Inject constructor(
    private val repository: RecurringTaskRepository,
    private val coordinator: RecurringTaskCoordinator
) : ToolSet {

    @Tool
    @LLMDescription("Create a new recurring task that runs automatically on a schedule. Returns the created task ID.")
    suspend fun createRecurringTask(
        @LLMDescription("Short descriptive title for the task") title: String,
        @LLMDescription("Detailed prompt describing what the agent should do when this task runs") prompt: String,
        @LLMDescription("Hour to run (0-23, 24-hour format)") hour: Int,
        @LLMDescription("Minute to run (0-59)") minute: Int,
        @LLMDescription("Comma-separated days of week: 1=Mon,2=Tue,3=Wed,4=Thu,5=Fri,6=Sat,7=Sun. Empty string for every day.") daysOfWeek: String = "",
        @LLMDescription("Human-readable schedule description, e.g. 'Every day at 9:00 AM' or 'Weekdays at 8:30 AM'") scheduleDisplay: String,
        @LLMDescription("Comma-separated package names of related apps, e.g. 'com.whatsapp,com.instagram'. Empty if none.") relatedApps: String = "",
        @LLMDescription("True for time-sensitive tasks (alarms, reminders, scheduled messages). False for flexible tasks (summaries, monitoring, research). Default false.") isTimeSensitive: Boolean = false
    ): String {
        Log.i(TAG, "Tool: createRecurringTask(title='$title', hour=$hour, minute=$minute, days='$daysOfWeek', timeSensitive=$isTimeSensitive)")

        val days = if (daysOfWeek.isBlank()) emptyList() else daysOfWeek.split(",").map { it.trim().toInt() }
        val apps = if (relatedApps.isBlank()) emptyList() else relatedApps.split(",").map { it.trim() }

        val task = RecurringTask(
            title = title,
            prompt = prompt,
            hour = hour,
            minute = minute,
            daysOfWeek = days,
            scheduleDisplay = scheduleDisplay,
            relatedApps = apps,
            isTimeSensitive = isTimeSensitive,
            createdAt = System.currentTimeMillis()
        )

        val id = coordinator.createAndSchedule(task)
        Log.i(TAG, "Tool: createRecurringTask() -> created with id=$id")
        return "Created recurring task '$title' (id=$id) — $scheduleDisplay"
    }

    @Tool
    @LLMDescription("List all recurring tasks with their status, schedule, and last run info.")
    suspend fun listRecurringTasks(): String {
        Log.i(TAG, "Tool: listRecurringTasks() called")
        val allTasks = repository.getAll()

        if (allTasks.isEmpty()) {
            Log.i(TAG, "Tool: listRecurringTasks() -> no tasks")
            return "No recurring tasks configured."
        }

        val result = allTasks.joinToString("\n") { task ->
            val status = if (task.enabled) "enabled" else "disabled"
            val lastRun = if (task.lastRunAt != null) "last run: ${task.lastRunStatus ?: "unknown"}" else "never run"
            "[id=${task.id}] ${task.title} — ${task.scheduleDisplay} ($status, $lastRun)"
        }

        Log.i(TAG, "Tool: listRecurringTasks() -> ${allTasks.size} tasks")
        return result
    }

    @Tool
    @LLMDescription("Update an existing recurring task. Only provide the fields you want to change.")
    suspend fun updateRecurringTask(
        @LLMDescription("ID of the task to update") taskId: Long,
        @LLMDescription("New title, or empty string to keep current") title: String = "",
        @LLMDescription("New prompt, or empty string to keep current") prompt: String = "",
        @LLMDescription("New hour (0-23), or -1 to keep current") hour: Int = -1,
        @LLMDescription("New minute (0-59), or -1 to keep current") minute: Int = -1,
        @LLMDescription("New days of week (comma-separated), or 'keep' to keep current") daysOfWeek: String = "keep",
        @LLMDescription("New schedule display text, or empty string to keep current") scheduleDisplay: String = "",
        @LLMDescription("Set to true to enable, false to disable") enabled: Boolean = true
    ): String {
        Log.i(TAG, "Tool: updateRecurringTask(taskId=$taskId)")

        val existing = repository.getById(taskId)
            ?: return "FAILED: No task found with id=$taskId".also { Log.w(TAG, it) }

        val updated = existing.copy(
            title = if (title.isNotBlank()) title else existing.title,
            prompt = if (prompt.isNotBlank()) prompt else existing.prompt,
            hour = if (hour >= 0) hour else existing.hour,
            minute = if (minute >= 0) minute else existing.minute,
            daysOfWeek = if (daysOfWeek != "keep") {
                if (daysOfWeek.isBlank()) emptyList() else daysOfWeek.split(",").map { it.trim().toInt() }
            } else existing.daysOfWeek,
            scheduleDisplay = if (scheduleDisplay.isNotBlank()) scheduleDisplay else existing.scheduleDisplay,
            enabled = enabled
        )

        coordinator.updateAndReschedule(updated)
        Log.i(TAG, "Tool: updateRecurringTask() -> updated task $taskId")
        return "Updated task '${updated.title}' (id=$taskId)"
    }

    @Tool
    @LLMDescription("Delete a recurring task permanently by its ID.")
    suspend fun deleteRecurringTask(
        @LLMDescription("ID of the task to delete") taskId: Long
    ): String {
        Log.i(TAG, "Tool: deleteRecurringTask(taskId=$taskId)")

        val existing = repository.getById(taskId)
            ?: return "FAILED: No task found with id=$taskId".also { Log.w(TAG, it) }

        coordinator.deleteAndCancel(taskId)
        Log.i(TAG, "Tool: deleteRecurringTask() -> deleted task $taskId")
        return "Deleted task '${existing.title}' (id=$taskId)"
    }
}
