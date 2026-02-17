package com.aiassistant.data.di

import com.aiassistant.BuildConfig
import com.aiassistant.agent.LLMProvider
import com.aiassistant.data.remote.ApiKeyProvider
import com.aiassistant.domain.preference.SharedPreferenceDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

    @Provides
    @Singleton
    fun provideApiKeyProvider(preferences: SharedPreferenceDataSource): ApiKeyProvider {
        return BuildConfigApiKeyProvider(preferences)
    }
}

class BuildConfigApiKeyProvider(
    private val preferences: SharedPreferenceDataSource
) : ApiKeyProvider {

    override fun getApiKey(): String {
        return getApiKey(LLMProvider.OPENAI) ?: BuildConfig.OPENAI_API_KEY
    }

    override fun getApiKey(provider: LLMProvider): String? {
        val savedKey = preferences.getApiKey(provider)
        if (!savedKey.isNullOrBlank()) {
            return savedKey
        }
        // Fall back to BuildConfig only for OpenAI
        if (provider == LLMProvider.OPENAI) {
            val buildConfigKey = BuildConfig.OPENAI_API_KEY
            if (buildConfigKey.isNotBlank()) {
                return buildConfigKey
            }
        }
        return null
    }
}
