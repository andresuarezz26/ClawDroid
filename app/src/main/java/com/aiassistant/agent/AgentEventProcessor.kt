package com.aiassistant.agent

import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.message.FeatureMessageProcessor
import ai.koog.agents.core.feature.model.events.AgentCompletedEvent
import ai.koog.agents.core.feature.model.events.AgentExecutionFailedEvent
import ai.koog.agents.core.feature.model.events.AgentStartingEvent
import ai.koog.agents.core.feature.model.events.ToolCallStartingEvent
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class AgentEventProcessor(override val isOpen: StateFlow<Boolean>) : FeatureMessageProcessor() {
    private val _progress = MutableSharedFlow<AgentProgress>(replay = 1)
    val progress: SharedFlow<AgentProgress> = _progress.asSharedFlow()

    override suspend fun processMessage(message: FeatureMessage) {
        Log.i(TAG, "Received message: ${message::class.simpleName}")
        when (message) {
            is AgentStartingEvent -> {
                Log.i(TAG, "Agent starting")
                _progress.emit(AgentProgress.Started)
            }
            is ToolCallStartingEvent -> {
                Log.i(TAG, "Tool call starting: ${message.toolName}")
                _progress.emit(AgentProgress.ToolExecuting(message.toolName))
            }
            is AgentCompletedEvent -> {
                Log.i(TAG, "Agent completed with result: ${message.result}")
                _progress.emit(AgentProgress.Completed(message.result?.toString()))
            }
            is AgentExecutionFailedEvent -> {
                Log.i(TAG, "Agent execution failed: ${message.error?.message}")
                _progress.emit(AgentProgress.Failed(message.error?.message))
            }
            else -> {
                Log.i(TAG, "Ignoring message: ${message::class.simpleName}")
            }
        }
    }

    companion object {
        private const val TAG = "Agent"
    }


    override suspend fun close() {
    }
}

sealed interface AgentProgress {
    data object Started : AgentProgress
    data class ToolExecuting(val toolName: String) : AgentProgress
    data class Completed(val result: String?) : AgentProgress
    data class Failed(val error: String?) : AgentProgress
}
