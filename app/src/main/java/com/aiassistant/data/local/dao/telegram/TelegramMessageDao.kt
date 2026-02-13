package com.aiassistant.data.local.dao.telegram

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aiassistant.data.local.entity.telegram.TelegramMessageEntity

@Dao
interface TelegramMessageDao {

    @Insert
    suspend fun insert(message: TelegramMessageEntity)

    // Fetch the N most recent messages, then reverse to chronological order
    // so the LLM sees them oldest → newest (natural conversation flow)
    @Query("SELECT * FROM (SELECT * FROM telegram_messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    suspend fun getRecentMessages(chatId: Long, limit: Int): List<TelegramMessageEntity>
}
