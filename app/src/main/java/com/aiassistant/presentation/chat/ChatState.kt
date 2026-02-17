package com.aiassistant.presentation.chat

import com.aiassistant.agent.LLMProvider
import com.aiassistant.agent.ModelInfo
import com.aiassistant.agent.ModelRegistry
import com.aiassistant.domain.model.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isExecuting: Boolean = false,
    val currentStep: String = "",
    val currentTool: String? = null,
    val stepCount: Int = 0,
    val isServiceConnected: Boolean = false,
    val selectedModelId: String = ModelRegistry.defaultModel.id,
    val selectedModelDisplayName: String = ModelRegistry.defaultModel.displayName,
    val isModelDropdownExpanded: Boolean = false,
    val configuredProviders: Set<LLMProvider> = emptySet()
) {
    val availableModels: List<ModelInfo>
        get() = ModelRegistry.models.filter { it.provider in configuredProviders }

    val hasAnyApiKey: Boolean
        get() = configuredProviders.isNotEmpty()
}
