package com.aiassistant.domain.model

sealed interface TaskResult {
    data class Success(val summary: String, val stepsUsed: Int) : TaskResult
    data class Failed(val reason: String, val stepsUsed: Int) : TaskResult
    data object MaxStepsReached : TaskResult
    data object Cancelled : TaskResult
}