package com.aiassistant.framework.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject
import javax.inject.Singleton

interface AccessibilityServiceBridge {
    fun getRootNode(): AccessibilityNodeInfo?
    fun isConnected(): Boolean
    fun performGlobalAction(action: Int): Boolean
    fun getContext(): Context?
}

@Singleton
class AccessibilityServiceBridgeImpl @Inject constructor() : AccessibilityServiceBridge {

    override fun getRootNode(): AccessibilityNodeInfo? {
        return AutomatorAccessibilityService.getInstance()?.rootInActiveWindow
    }

    override fun isConnected(): Boolean {
        return AutomatorAccessibilityService.isConnected.value
    }

    override fun performGlobalAction(action: Int): Boolean {
        return AutomatorAccessibilityService.getInstance()
            ?.performGlobalAction(action) ?: false
    }

    override fun getContext(): Context? {
        return AutomatorAccessibilityService.getInstance()
    }
}