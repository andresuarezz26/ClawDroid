package com.aiassistant.framework.scheduler

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aiassistant.agent.AgentExecutor
import com.aiassistant.agent.AgentResult
import com.aiassistant.agent.AgentType
import com.aiassistant.domain.model.TaskExecution
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import com.aiassistant.domain.usecase.messages.SaveMessageUseCase
import com.aiassistant.domain.usecase.telegram.SendResponseTelegramUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

private const val TAG = "RecurringTaskWorker"

@HiltWorker
class RecurringTaskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: RecurringTaskRepository,
    private val agentExecutor: AgentExecutor,
    private val saveMessageUseCase: SaveMessageUseCase,
    private val sendResponseTelegramUseCase: SendResponseTelegramUseCase,
    private val scheduler: RecurringTaskScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId == -1L) {
            Log.w(TAG, "No task ID in input data")
            return Result.failure()
        }

        Log.i(TAG, "Starting work for task $taskId")

        val task = repository.getById(taskId)
        if (task == null) {
            Log.w(TAG, "Task $taskId not found in DB")
            return Result.failure()
        }

        if (!task.enabled) {
            Log.i(TAG, "Task $taskId is disabled, skipping")
            return Result.success()
        }

        // For flexible tasks with day-of-week restrictions, check if today is valid
        if (!task.isTimeSensitive && task.daysOfWeek.isNotEmpty()) {
            val calDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val ourDow = if (calDow == Calendar.SUNDAY) 7 else calDow - 1
            if (!task.daysOfWeek.contains(ourDow)) {
                Log.i(TAG, "Task $taskId skipped — today ($ourDow) not in ${task.daysOfWeek}")
                return Result.success()
            }
        }

        val startTime = System.currentTimeMillis()

        return try {
            val result = agentExecutor.execute(
                command = task.prompt,
                requireServiceConnection = false,
                agentType = AgentType.TASK_EXECUTOR
            )

            val durationMs = System.currentTimeMillis() - startTime
            val (status, summary) = when (result) {
                is AgentResult.Success -> "success" to result.response
                is AgentResult.Failure -> "failure" to result.reason
                is AgentResult.ServiceNotConnected -> "failure" to "Service not connected"
                is AgentResult.Cancelled -> "failure" to "Cancelled"
            }

            // Record execution
            val execution = TaskExecution(
                taskId = taskId,
                executedAt = System.currentTimeMillis(),
                status = status,
                summary = summary,
                durationMs = durationMs
            )
            repository.recordExecution(execution)
            repository.update(task.copy(
                lastRunAt = execution.executedAt,
                lastRunStatus = status,
                lastRunSummary = summary
            ))

            // Save to conversation history (app chat)
            val historyMsg = "[Recurring Task: ${task.title}]\n$summary"
            saveMessageUseCase(
                chatId = RECURRING_TASK_CHAT_ID,
                content = historyMsg,
                isFromUser = false
            )

            // Send to Telegram if possible
            try {
                sendResponseTelegramUseCase(
                    chatId = RECURRING_TASK_CHAT_ID,
                    text = historyMsg
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send to Telegram: ${e.message}")
            }

            // If time-sensitive, reschedule next alarm
            if (task.isTimeSensitive) {
                scheduler.scheduleTask(task)
            }

            Log.i(TAG, "Task $taskId completed: $status")
            Result.success()
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            Log.e(TAG, "Task $taskId failed: ${e.message}", e)

            val execution = TaskExecution(
                taskId = taskId,
                executedAt = System.currentTimeMillis(),
                status = "failure",
                summary = e.message,
                durationMs = durationMs
            )
            repository.recordExecution(execution)
            repository.update(task.copy(
                lastRunAt = execution.executedAt,
                lastRunStatus = "failure",
                lastRunSummary = e.message
            ))

            // Reschedule even on failure for time-sensitive tasks
            if (task.isTimeSensitive) {
                scheduler.scheduleTask(task)
            }

            Result.retry()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        private const val RECURRING_TASK_CHAT_ID = 1L
    }
}
