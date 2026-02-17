package com.aiassistant.framework.notification

import android.content.Context
import android.content.Intent
import android.util.Log
import com.aiassistant.agent.AgentExecutor
import com.aiassistant.agent.AgentResult
import com.aiassistant.agent.AgentType
import com.aiassistant.domain.model.DEFAULT_CHAT_ID
import com.aiassistant.domain.preference.SharedPreferenceDataSource
import com.aiassistant.domain.repository.notification.NotificationRepository
import com.aiassistant.domain.usecase.messages.GetConversationHistoryUseCase
import com.aiassistant.domain.usecase.messages.SaveMessageUseCase
import com.aiassistant.domain.usecase.telegram.SendResponseTelegramUseCase
import com.aiassistant.framework.telegram.TelegramServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationReactor"
private const val RATE_LIMIT_MS = 15_000L

@Singleton
class NotificationReactor @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: NotificationRepository,
    private val agentExecutor: AgentExecutor,
    private val sendResponseTelegramUseCase: SendResponseTelegramUseCase,
    private val saveMessageUseCase: SaveMessageUseCase,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
    private val telegramServiceState: TelegramServiceState,
    private val preferences: SharedPreferenceDataSource
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null
    private var lastAgentExecutionTime = 0L

    fun startReacting() {
        if (collectionJob?.isActive == true) return

        Log.i(TAG, "Starting notification reactor")
        collectionJob = scope.launch {
            repository.incomingNotifications.collect { notification ->
                try {
                    // Check if forwarding is enabled
                    if (!preferences.getNotificationForwardingEnabled()) return@collect

                    // Check package filter
                    val filterPackages = preferences.getNotificationFilterPackages()
                    if (filterPackages.isNotEmpty() && notification.packageName !in filterPackages) return@collect

                    // Rate limit
                    val now = System.currentTimeMillis()
                    if (now - lastAgentExecutionTime < RATE_LIMIT_MS) {
                        Log.d(TAG, "Rate limited, skipping notification from ${notification.appName}")
                        return@collect
                    }
                    lastAgentExecutionTime = now

                    val prompt = buildString {
                        appendLine("[NOTIFICATION from ${notification.appName}]")
                        if (!notification.title.isNullOrBlank()) {
                            appendLine("Title: ${notification.title}")
                        }
                        if (!notification.text.isNullOrBlank()) {
                            appendLine("Message: ${notification.text}")
                        }
                        if (notification.hasReplyAction && notification.notificationKey != null) {
                            appendLine("NotificationKey: ${notification.notificationKey}")
                            appendLine("ReplyCapable: YES - You can use replyToNotification(\"${notification.notificationKey}\", \"your reply text\") to reply directly.")
                        }
                        append("Do NOT just summarize the notification — take action when possible.")
                    }

                    Log.i(TAG, "Processing notification from ${notification.appName}")

                    // Save as user message to unified conversation
                    saveMessageUseCase(DEFAULT_CHAT_ID, prompt, isFromUser = true)

                    // Get conversation history from unified conversation
                    val history = getConversationHistoryUseCase(DEFAULT_CHAT_ID, limit = 20)

                    // Execute agent with UI automation enabled
                    var result = agentExecutor.execute(
                        command = prompt,
                        conversationHistory = history,
                        requireServiceConnection = true,
                        agentType = AgentType.NOTIFICATION_REACTOR
                    )

                    // Fallback: if accessibility service is not connected, retry with quick actions only
                    if (result is AgentResult.ServiceNotConnected) {
                        Log.i(TAG, "Service not connected, falling back to quick actions only")
                        result = agentExecutor.execute(
                            command = prompt,
                            conversationHistory = history,
                            requireServiceConnection = false,
                            agentType = AgentType.NOTIFICATION_REACTOR
                        )
                    }

                    val responseText = when (result) {
                        is AgentResult.Success -> result.response
                        is AgentResult.Failure -> "Could not process notification: ${result.reason}"
                        is AgentResult.ServiceNotConnected -> "Notification from ${notification.appName}: ${notification.title} - ${notification.text}"
                        is AgentResult.Cancelled -> return@collect
                    }

                    // Save response to unified conversation
                    saveMessageUseCase(DEFAULT_CHAT_ID, responseText, isFromUser = false)

                    // Send to Telegram only if running and chat ID is available
                    if (telegramServiceState.isRunning.value) {
                        val telegramChatId = preferences.getTelegramChatId()
                        if (telegramChatId != null) {
                            sendResponseTelegramUseCase(telegramChatId, responseText).onFailure { error ->
                                Log.e(TAG, "Failed to send notification response to Telegram", error)
                            }
                        }
                    }
                    // Return to ClawDroid app after agent finishes
                    bringAppToForeground()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification", e)
                }
            }
        }
    }

    private fun bringAppToForeground() {
        try {
            val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                appContext.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring app to foreground", e)
        }
    }

    fun stopReacting() {
        Log.i(TAG, "Stopping notification reactor")
        collectionJob?.cancel()
        collectionJob = null
    }
}
