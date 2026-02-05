package com.aiassistant.presentation.chat

sealed interface ChatSideEffect {
    data class ShowError(val message: String) : ChatSideEffect
    data object ScrollToBottom : ChatSideEffect
    data object OpenAccessibilitySettings : ChatSideEffect

    object BringToForeground : ChatSideEffect
}