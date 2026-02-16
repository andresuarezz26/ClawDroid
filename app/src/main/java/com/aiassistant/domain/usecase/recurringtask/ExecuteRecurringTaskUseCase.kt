package com.aiassistant.domain.usecase.recurringtask

import com.aiassistant.agent.AgentExecutor
import com.aiassistant.agent.AgentResult
import com.aiassistant.agent.AgentType
import com.aiassistant.domain.model.TaskExecution
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import javax.inject.Inject

class ExecuteRecurringTaskUseCase @Inject constructor(
    private val repository: RecurringTaskRepository,
    private val agentExecutor: AgentExecutor
) {
    suspend operator fun invoke(taskId: Long): TaskExecution? {

        val task = repository.getById(taskId) ?: return null

        val startTime = System.currentTimeMillis()
        val result = agentExecutor.execute(
            command = task.prompt,
            agentType = AgentType.TASK_EXECUTOR,
            requireServiceConnection = false
        )
        val durationMs = System.currentTimeMillis() - startTime

        val (status, summary) = when (result) {
            is AgentResult.Success -> "success" to result.response
            is AgentResult.Failure -> "failure" to result.reason
            is AgentResult.ServiceNotConnected -> "failure" to "Service not connected"
            is AgentResult.Cancelled -> "failure" to "Cancelled"
        }

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

        return execution
    }
}
