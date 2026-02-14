package com.aiassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_conversations")
data class ConversationEntity(
    @PrimaryKey
    val chatId: Long,
    val username: String?,
    val firstName: String?,
    val lastMessageAt: Long
)