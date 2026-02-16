package com.aiassistant.presentation.recurring_task_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aiassistant.domain.model.RecurringTask
import com.aiassistant.domain.model.TaskExecution
import com.aiassistant.domain.usecase.recurringtask.ExecuteRecurringTaskUseCase
import com.aiassistant.domain.usecase.recurringtask.GetRecurringTaskByIdUseCase
import com.aiassistant.domain.usecase.recurringtask.GetTaskExecutionHistoryUseCase
import com.aiassistant.framework.scheduler.RecurringTaskCoordinator
import com.aiassistant.presentation.navigation.NavigationScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringTaskDetailState(
    val task: RecurringTask? = null,
    val executions: List<TaskExecution> = emptyList(),
    val isLoading: Boolean = true,
    val isRunning: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val editTitle: String = "",
    val editPrompt: String = "",
    val editHour: Int = 9,
    val editMinute: Int = 0,
    val editDaysOfWeek: List<Int> = emptyList(),
    val isEdited: Boolean = false
)

sealed interface RecurringTaskDetailIntent {
    data class UpdateTitle(val title: String) : RecurringTaskDetailIntent
    data class UpdatePrompt(val prompt: String) : RecurringTaskDetailIntent
    data class UpdateHour(val hour: Int) : RecurringTaskDetailIntent
    data class UpdateMinute(val minute: Int) : RecurringTaskDetailIntent
    data class ToggleDay(val day: Int) : RecurringTaskDetailIntent
    data class ToggleEnabled(val enabled: Boolean) : RecurringTaskDetailIntent
    object Save : RecurringTaskDetailIntent
    object RunNow : RecurringTaskDetailIntent
    object ShowDeleteDialog : RecurringTaskDetailIntent
    object DismissDeleteDialog : RecurringTaskDetailIntent
    object ConfirmDelete : RecurringTaskDetailIntent
}

@HiltViewModel
class RecurringTaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskById: GetRecurringTaskByIdUseCase,
    private val getExecutionHistory: GetTaskExecutionHistoryUseCase,
    private val executeTask: ExecuteRecurringTaskUseCase,
    private val coordinator: RecurringTaskCoordinator
) : ViewModel() {

    private val route = savedStateHandle.toRoute<NavigationScreen.RecurringTaskDetail>()
    private val taskId = route.taskId

    private val _state = MutableStateFlow(RecurringTaskDetailState())
    val state = _state.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = getTaskById(taskId)
            val executions = getExecutionHistory(taskId)
            if (task != null) {
                _state.update {
                    it.copy(
                        task = task,
                        executions = executions,
                        isLoading = false,
                        editTitle = task.title,
                        editPrompt = task.prompt,
                        editHour = task.hour,
                        editMinute = task.minute,
                        editDaysOfWeek = task.daysOfWeek,
                        isEdited = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun processIntent(intent: RecurringTaskDetailIntent) {
        when (intent) {
            is RecurringTaskDetailIntent.UpdateTitle -> {
                _state.update { it.copy(editTitle = intent.title, isEdited = true) }
            }
            is RecurringTaskDetailIntent.UpdatePrompt -> {
                _state.update { it.copy(editPrompt = intent.prompt, isEdited = true) }
            }
            is RecurringTaskDetailIntent.UpdateHour -> {
                _state.update { it.copy(editHour = intent.hour, isEdited = true) }
            }
            is RecurringTaskDetailIntent.UpdateMinute -> {
                _state.update { it.copy(editMinute = intent.minute, isEdited = true) }
            }
            is RecurringTaskDetailIntent.ToggleDay -> {
                _state.update { current ->
                    val days = current.editDaysOfWeek.toMutableList()
                    if (days.contains(intent.day)) days.remove(intent.day) else days.add(intent.day)
                    current.copy(editDaysOfWeek = days.sorted(), isEdited = true)
                }
            }
            is RecurringTaskDetailIntent.ToggleEnabled -> {
                viewModelScope.launch {
                    coordinator.toggleAndReschedule(taskId, intent.enabled)
                    loadTask()
                }
            }
            RecurringTaskDetailIntent.Save -> save()
            RecurringTaskDetailIntent.RunNow -> runNow()
            RecurringTaskDetailIntent.ShowDeleteDialog -> {
                _state.update { it.copy(showDeleteConfirmation = true) }
            }
            RecurringTaskDetailIntent.DismissDeleteDialog -> {
                _state.update { it.copy(showDeleteConfirmation = false) }
            }
            RecurringTaskDetailIntent.ConfirmDelete -> delete()
        }
    }

    private fun save() {
        val current = _state.value.task ?: return
        val s = _state.value
        val daysDisplay = if (s.editDaysOfWeek.isEmpty()) "Every day" else {
            val names = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
            s.editDaysOfWeek.mapNotNull { names[it] }.joinToString(", ")
        }
        val timeDisplay = String.format("%d:%02d %s",
            if (s.editHour == 0) 12 else if (s.editHour > 12) s.editHour - 12 else s.editHour,
            s.editMinute,
            if (s.editHour < 12) "AM" else "PM"
        )
        val updated = current.copy(
            title = s.editTitle,
            prompt = s.editPrompt,
            hour = s.editHour,
            minute = s.editMinute,
            daysOfWeek = s.editDaysOfWeek,
            scheduleDisplay = "$daysDisplay at $timeDisplay"
        )
        viewModelScope.launch {
            coordinator.updateAndReschedule(updated)
            _state.update { it.copy(task = updated, isEdited = false) }
        }
    }

    private fun runNow() {
        _state.update { it.copy(isRunning = true) }
        viewModelScope.launch {
            executeTask(taskId)
            val executions = getExecutionHistory(taskId)
            val task = getTaskById(taskId)
            _state.update {
                it.copy(
                    task = task,
                    executions = executions,
                    isRunning = false
                )
            }
        }
    }

    private fun delete() {
        viewModelScope.launch {
            coordinator.deleteAndCancel(taskId)
            _state.update { it.copy(task = null, showDeleteConfirmation = false) }
        }
    }
}
