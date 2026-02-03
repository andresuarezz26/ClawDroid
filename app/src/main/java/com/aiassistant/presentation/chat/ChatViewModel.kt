package com.aiassistant.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.model.TaskStatus
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.usecase.ExecuteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val executeTaskUseCase: ExecuteTaskUseCase,
    private val screenRepository: ScreenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _sideEffect = Channel<ChatSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<ChatSideEffect> = _sideEffect.receiveAsFlow()

    private var executionJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                _state.update { it.copy(isServiceConnected = screenRepository.isServiceConnected()) }
                delay(2000)
            }
        }
    }

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> {
                _state.update { it.copy(inputText = intent.input) }
            }
            is ChatIntent.ExecuteCommand -> executeCommand(intent.command)
            is ChatIntent.CancelExecution -> cancelExecution()
            is ChatIntent.ClearHistory -> {
                _state.update { it.copy(messages = emptyList()) }
            }
        }
    }

    private fun executeCommand(command: String) {
        if (command.isBlank()) return
        if (!_state.value.isServiceConnected) {
            viewModelScope.launch {
                _sideEffect.send(ChatSideEffect.OpenAccessibilitySettings)
            }
            return
        }

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(content = command, isUser = true),
                inputText = "",
                isExecuting = true,
                currentStep = "Starting...",
                stepCount = 0
            )
        }
        _sideEffect.trySend(ChatSideEffect.ScrollToBottom)

        executionJob = viewModelScope.launch {
            try {
                executeTaskUseCase(command).collect { stepResult ->
                    _state.update {
                        it.copy(
                            currentStep = stepResult.thought,
                            stepCount = stepResult.stepNumber + 1
                        )
                    }

                    if (stepResult.status == TaskStatus.DONE ||
                        stepResult.status == TaskStatus.FAILED) {

                        val resultText = when (stepResult.status) {
                            TaskStatus.DONE -> "Done! ${stepResult.thought}"
                            TaskStatus.FAILED -> "Failed: ${stepResult.thought}"
                            else -> stepResult.thought
                        }
                        _state.update {
                            it.copy(
                                messages = it.messages + ChatMessage(
                                    content = resultText,
                                    isUser = false
                                ),
                                isExecuting = false
                            )
                        }
                        _sideEffect.send(ChatSideEffect.ScrollToBottom)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            content = "Task cancelled.",
                            isUser = false
                        ),
                        isExecuting = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isExecuting = false) }
                _sideEffect.send(ChatSideEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun cancelExecution() {
        executionJob?.cancel()
        _state.update { it.copy(isExecuting = false, currentStep = "Cancelled") }
    }
}