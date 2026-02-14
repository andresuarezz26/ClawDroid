package com.aiassistant.domain.usecase.messages

import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.repository.messages.MessagesRepository
import javax.inject.Inject

class GetConversationHistoryUseCase @Inject constructor(
    private val repository: MessagesRepository
) {
    suspend operator fun invoke(chatId: Long, limit: Int = 20): List<ChatMessage> {
        return repository.getConversationHistory(chatId, limit)
    }
}