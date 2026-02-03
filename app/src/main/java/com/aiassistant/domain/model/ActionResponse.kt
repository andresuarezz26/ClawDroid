package com.aiassistant.domain.model

data class ActionResponse(
    val thought: String,
    val status: TaskStatus,
    val actions: List<Action>
)

enum class TaskStatus { ACTING, DONE, FAILED }