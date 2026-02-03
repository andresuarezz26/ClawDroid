package com.aiassistant.presentation.chat

import com.aiassistant.domain.model.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isExecuting: Boolean = false,
    val currentStep: String = "",
    val stepCount: Int = 0,
    val isServiceConnected: Boolean = false
)