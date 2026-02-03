package com.aiassistant.domain.model

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class AppNotFound(val packageName: String) : LaunchResult
    data class FoundAndLaunched(val packageName: String, val displayName: String) : LaunchResult
}
