package com.aiassistant.agent.di

import com.aiassistant.agent.AndroidAgentFactory
import com.aiassistant.agent.DeviceTools
import com.aiassistant.agent.QuickActionTools
import com.aiassistant.data.mapper.UINodeFormatter
import com.aiassistant.domain.repository.ScreenRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideDeviceTools(
        screenRepository: ScreenRepository,
        uiNodeFormatter: UINodeFormatter
    ): DeviceTools {
        return DeviceTools(screenRepository, uiNodeFormatter)
    }

    @Provides
    @Singleton
    fun provideQuickActionTools(
        screenRepository: ScreenRepository
    ): QuickActionTools {
        return QuickActionTools(screenRepository)
    }

    @Provides
    @Singleton
    fun provideAndroidAgentFactory(
        deviceTools: DeviceTools,
        quickActionTools: QuickActionTools
    ): AndroidAgentFactory {
        return AndroidAgentFactory(deviceTools, quickActionTools)
    }
}
