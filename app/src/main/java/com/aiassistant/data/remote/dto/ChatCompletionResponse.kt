package com.aiassistant.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChoiceDto>
)

@Serializable
data class ChoiceDto(
    val message: MessageDto
)