package com.aiassistant.framework.di

import com.aiassistant.framework.accessibility.AccessibilityServiceBridge
import com.aiassistant.framework.accessibility.AccessibilityServiceBridgeImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FrameworkModule {

    @Binds
    @Singleton
    abstract fun bindServiceBridge(impl: AccessibilityServiceBridgeImpl): AccessibilityServiceBridge
}