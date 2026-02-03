package com.aiassistant.domain.repository

import com.aiassistant.domain.model.ActionResponse
import com.aiassistant.domain.model.UINode

interface LlmRepository {
    suspend fun getNextActions(
        screenState: List<UINode>,
        userCommand: String,
        conversationHistory: List<Pair<String, String>>
    ): ActionResponse
}