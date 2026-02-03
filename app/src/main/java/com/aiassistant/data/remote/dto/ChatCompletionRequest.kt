package com.aiassistant.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<MessageDto>,
    @SerialName("response_format") val responseFormat: ResponseFormatDto,
    val temperature: Double
)

@Serializable
data class MessageDto(val role: String, val content: String)

@Serializable
data class ResponseFormatDto(val type: String)