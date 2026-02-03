package com.aiassistant.domain.usecase

import com.aiassistant.domain.model.StepResult
import com.aiassistant.domain.model.TaskStatus
import com.aiassistant.domain.repository.LlmRepository
import com.aiassistant.domain.repository.ScreenRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ExecuteTaskUseCase @Inject constructor(
    private val screenRepo: ScreenRepository,
    private val llmRepo: LlmRepository
) {
    companion object {
        private const val MAX_STEPS = 20
        private const val SETTLE_DELAY_MS = 1500L
    }

    operator fun invoke(command: String): Flow<StepResult> = flow {
        val history = mutableListOf<Pair<String, String>>()
        var stepCount = 0

        while (stepCount < MAX_STEPS) {
            currentCoroutineContext().ensureActive()

            val screen = screenRepo.captureScreen()
            if (screen.isEmpty()) {
                emit(StepResult(stepCount, "Cannot read screen", emptyList(), TaskStatus.FAILED))
                return@flow
            }

            val response = llmRepo.getNextActions(screen, command, history)

            emit(StepResult(stepCount, response.thought, response.actions, response.status))

            if (response.status == TaskStatus.DONE || response.status == TaskStatus.FAILED) {
                return@flow
            }

            for (action in response.actions) {
                val success = screenRepo.performAction(action)
                if (!success) {
                    history.add("user" to "Action ${action.type} on index ${action.index} FAILED")
                }
            }

            delay(SETTLE_DELAY_MS)
            history.add("assistant" to response.thought)
            stepCount++
        }

        emit(StepResult(stepCount, "Maximum steps reached", emptyList(), TaskStatus.FAILED))
    }
}