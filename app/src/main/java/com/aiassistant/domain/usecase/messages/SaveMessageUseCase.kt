package com.aiassistant.domain.usecase.messages

import com.aiassistant.domain.repository.messages.MessagesRepository
import com.aiassistant.domain.repository.telegram.TelegramRepository
import javax.inject.Inject

class SaveMessageUseCase @Inject constructor(
    private val repository: MessagesRepository
) {
    suspend operator fun invoke(chatId: Long, content: String, isFromUser: Boolean) {
        repository.saveMessage(chatId, content, isFromUser)
    }
}