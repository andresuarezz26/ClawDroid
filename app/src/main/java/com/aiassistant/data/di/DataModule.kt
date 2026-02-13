package com.aiassistant.data.di

import android.content.Context
import androidx.room.Room
import com.aiassistant.data.local.AppDatabase
import com.aiassistant.data.local.dao.notification.NotificationDao
import com.aiassistant.data.local.dao.telegram.TelegramConversationDao
import com.aiassistant.data.local.dao.telegram.TelegramMessageDao
import com.aiassistant.data.repository.AppRepositoryImpl
import com.aiassistant.data.repository.ScreenRepositoryImpl
import com.aiassistant.data.repository.notification.NotificationRepositoryImpl
import com.aiassistant.data.repository.telegram.TelegramRepositoryImpl
import com.aiassistant.domain.repository.AppRepository
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.repository.notification.NotificationRepository
import com.aiassistant.domain.repository.telegram.TelegramRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindScreenRepository(impl: ScreenRepositoryImpl): ScreenRepository

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindTelegramRepository(impl: TelegramRepositoryImpl): TelegramRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "clawdroid_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramConversationDao(database: AppDatabase): TelegramConversationDao {
        return database.telegramConversationDao()
    }

    @Provides
    @Singleton
    fun provideTelegramMessageDao(database: AppDatabase): TelegramMessageDao {
        return database.telegramMessageDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }
}
