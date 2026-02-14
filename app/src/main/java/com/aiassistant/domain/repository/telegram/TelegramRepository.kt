package com.aiassistant.domain.repository.telegram

import com.aiassistant.domain.model.TelegramUpdate

interface TelegramRepository {
    suspend fun pollUpdates(offset: Long?): Result<List<TelegramUpdate>>
    suspend fun sendResponse(chatId: Long, text: String): Result<Unit>
    suspend fun validateToken(): Boolean
}

data class ConversationModel(
    val chatId: Long,
    val username: String?,
    val firstName: String?,
    val lastMessageAt: Long
)
