package com.aiassistant.presentation.recurring_task_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.domain.usecase.recurringtask.ObserveRecurringTasksUseCase
import com.aiassistant.framework.scheduler.RecurringTaskCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class RecurringTaskListViewModel @Inject constructor(
    private val observeRecurringTasks: ObserveRecurringTasksUseCase,
    private val coordinator: RecurringTaskCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringTaskListState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeRecurringTasks().collect { tasks ->
                _state.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    fun processIntent(intent: RecurringTaskListIntent) {
        when (intent) {
            is RecurringTaskListIntent.ToggleTask -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        coordinator.toggleAndReschedule(intent.taskId, intent.enabled)
                    }
                }
            }
        }
    }
}
