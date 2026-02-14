package com.aiassistant.domain.usecase.messages

import com.aiassistant.domain.repository.messages.MessagesRepository
import com.aiassistant.domain.repository.telegram.ConversationModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveConversationUseCase @Inject constructor(private val repository: MessagesRepository) {

  operator fun invoke(): Flow<List<ConversationModel>> {
    return repository.observeConversations()
  }
}