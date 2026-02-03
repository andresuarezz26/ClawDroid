package com.aiassistant.data.repository

import com.aiassistant.domain.model.ActionResponse
import com.aiassistant.domain.model.UINode
import com.aiassistant.domain.repository.LlmRepository
import com.aiassistant.data.mapper.UINodeFormatter
import com.aiassistant.data.mapper.ActionResponseMapper
import com.aiassistant.data.remote.OpenAIApiService
import com.aiassistant.data.remote.dto.ChatCompletionRequest
import com.aiassistant.data.remote.dto.MessageDto
import com.aiassistant.data.remote.dto.ResponseFormatDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val apiService: OpenAIApiService,
    private val formatter: UINodeFormatter,
    private val responseMapper: ActionResponseMapper
) : LlmRepository {

    override suspend fun getNextActions(
        screenState: List<UINode>,
        userCommand: String,
        conversationHistory: List<Pair<String, String>>
    ): ActionResponse {
        val screenText = formatter.format(screenState)

        val messages = buildList {
            add(MessageDto("system", SYSTEM_PROMPT))
            conversationHistory.forEach { (role, content) ->
                add(MessageDto(role, content))
            }
            add(MessageDto("user", "SCREEN STATE:\n$screenText\n\nUSER COMMAND: $userCommand"))
        }

        val request = ChatCompletionRequest(
            model = "gpt-4o",
            messages = messages,
            responseFormat = ResponseFormatDto(type = "json_object"),
            temperature = 0.1
        )

        val response = apiService.createCompletion(request)
        return responseMapper.toDomain(response)
    }
}