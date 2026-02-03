package com.aiassistant.domain.repository

import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.UINode

interface ScreenRepository {
    suspend fun captureScreen(): List<UINode>
    suspend fun performAction(action: Action): Boolean
    fun isServiceConnected(): Boolean
}