package com.aiassistant.data.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aiassistant.BuildConfig
import com.aiassistant.data.remote.ApiKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

    @Provides
    @Singleton
    fun provideApiKeyProvider(@ApplicationContext context: Context): ApiKeyProvider {
        return BuildConfigApiKeyProvider(context)
    }
}

class BuildConfigApiKeyProvider(context: Context) : ApiKeyProvider {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "clawdroid_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("clawdroid_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun getApiKey(): String {
        // First check SharedPreferences for user-set key
        val savedKey = prefs.getString(API_KEY_PREF, null)
        if (!savedKey.isNullOrBlank()) {
            return savedKey
        }
        // Fall back to BuildConfig key from local.properties
        return BuildConfig.OPENAI_API_KEY
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(API_KEY_PREF, apiKey).apply()
    }

    companion object {
        private const val API_KEY_PREF = "openai_api_key"
    }
}
