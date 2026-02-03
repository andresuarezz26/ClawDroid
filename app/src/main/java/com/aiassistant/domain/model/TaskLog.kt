package com.aiassistant.domain.model

data class TaskLog(
    val id: Long,
    val command: String,
    val result: String,
    val resultSummary: String,
    val timestamp: Long
)