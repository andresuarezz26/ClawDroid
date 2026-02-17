package com.aiassistant.data.remote

import com.aiassistant.agent.LLMProvider

interface ApiKeyProvider {
    fun getApiKey(): String
    fun getApiKey(provider: LLMProvider): String?
}
