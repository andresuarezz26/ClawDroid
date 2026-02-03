package com.aiassistant.data.remote

import com.aiassistant.data.remote.dto.ChatCompletionRequest
import com.aiassistant.data.remote.dto.ChatCompletionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIApiService @Inject constructor(
    private val client: HttpClient,
    private val apiKeyProvider: ApiKeyProvider
) {
    suspend fun createCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
        return client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${apiKeyProvider.getApiKey()}")
            setBody(request)
        }.body()
    }
}

interface ApiKeyProvider {
    fun getApiKey(): String
}