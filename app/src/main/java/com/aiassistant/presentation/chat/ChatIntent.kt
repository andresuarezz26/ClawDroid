package com.aiassistant.presentation.chat

sealed interface ChatIntent {
    data class ExecuteCommand(val command: String) : ChatIntent
    data object CancelExecution : ChatIntent
    data object ClearHistory : ChatIntent
    data class UpdateInput(val input: String) : ChatIntent
    data object ToggleModelDropdown : ChatIntent
    data class SelectModel(val modelId: String) : ChatIntent
}
