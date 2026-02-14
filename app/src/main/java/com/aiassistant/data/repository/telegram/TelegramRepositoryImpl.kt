package com.aiassistant.data.repository.telegram

import com.aiassistant.data.local.dao.telegram.ConversationDao
import com.aiassistant.data.local.entity.ConversationEntity
import com.aiassistant.data.remote.telegram.TelegramApi
import com.aiassistant.domain.model.TelegramUpdate
import com.aiassistant.domain.repository.telegram.TelegramRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramRepositoryImpl @Inject constructor(
  private val telegramApi: TelegramApi,
  private val conversationDao: ConversationDao
) : TelegramRepository {

    override suspend fun pollUpdates(offset: Long?): Result<List<TelegramUpdate>> {
        return runCatching {
            val updates = telegramApi.getUpdates(offset)
            updates.mapNotNull { update ->
                val message = update.message ?: return@mapNotNull null
                val text = message.text ?: return@mapNotNull null

                // Update conversation record
                val conversation = ConversationEntity(
                  chatId = message.chat.id,
                  username = message.from?.username,
                  firstName = message.from?.firstName,
                  lastMessageAt = System.currentTimeMillis()
                )
                conversationDao.upsert(conversation)

              TelegramUpdate(
                updateId = update.updateId,
                chatId = message.chat.id,
                userId = message.from?.id ?: 0L,
                username = message.from?.username,
                firstName = message.from?.firstName,
                text = text,
                timestamp = message.date * 1000L
              )
            }
        }
    }

    override suspend fun sendResponse(chatId: Long, text: String): Result<Unit> {
        return runCatching {
            val chunks = splitMessage(text)
            for (chunk in chunks) {
                telegramApi.sendMessage(chatId, chunk)
            }
        }
    }

    private fun splitMessage(text: String, maxLength: Int = 4096): List<String> {
        if (text.length <= maxLength) return listOf(text)

        val chunks = mutableListOf<String>()
        var remaining = text

        while (remaining.isNotEmpty()) {
            if (remaining.length <= maxLength) {
                chunks.add(remaining)
                break
            }

            // Try to split at last newline within limit
            val splitIndex = remaining.lastIndexOf('\n', maxLength)
                .takeIf { it > maxLength / 2 }
            // Otherwise split at last space
                ?: remaining.lastIndexOf(' ', maxLength)
                    .takeIf { it > maxLength / 2 }
            // Hard split as last resort
                ?: maxLength

            chunks.add(remaining.substring(0, splitIndex).trimEnd())
            remaining = remaining.substring(splitIndex).trimStart()
        }

        return chunks
    }

    override suspend fun validateToken(): Boolean {
        return runCatching {
            telegramApi.getMe()
            true
        }.getOrDefault(false)
    }
}