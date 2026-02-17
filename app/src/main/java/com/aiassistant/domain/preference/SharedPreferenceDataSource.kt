package com.aiassistant.domain.preference

import com.aiassistant.agent.LLMProvider

interface SharedPreferenceDataSource {
  fun getToken(): String?
  fun setToken(token: String)
  fun clearToken()
  fun hasToken(): Boolean
  fun getNotificationForwardingEnabled(): Boolean
  fun setNotificationForwardingEnabled(enabled: Boolean)
  fun getNotificationFilterPackages(): Set<String>
  fun setNotificationFilterPackages(packages: Set<String>)
  fun getTelegramChatId(): Long?
  fun setTelegramChatId(chatId: Long)

  // Model selection
  fun getSelectedModelId(): String?
  fun setSelectedModelId(modelId: String)

  // Per-provider API keys
  fun getApiKey(provider: LLMProvider): String?
  fun setApiKey(provider: LLMProvider, key: String)
  fun clearApiKey(provider: LLMProvider)
  fun hasApiKey(provider: LLMProvider): Boolean
}