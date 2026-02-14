package com.aiassistant.domain.repository.messages

import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.repository.telegram.ConversationModel
import kotlinx.coroutines.flow.Flow

/**
 * Manage conversations and messages. A conversation is a list of messages from a session chat
 * For now we will keep a single conversation for all the different chats.
 */
interface MessagesRepository {
  suspend fun getConversationHistory(chatId: Long, limit: Int): List<ChatMessage>
  suspend fun saveMessage(chatId: Long, content: String, isFromUser: Boolean)

  fun observeConversations(): Flow<List<ConversationModel>>
}