package com.aiassistant.domain.model

data class StepResult(
    val stepNumber: Int,
    val thought: String,
    val actionsTaken: List<Action>,
    val status: TaskStatus
)