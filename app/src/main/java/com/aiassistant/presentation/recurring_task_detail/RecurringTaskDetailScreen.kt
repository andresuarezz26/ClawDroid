package com.aiassistant.presentation.recurring_task_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiassistant.domain.model.TaskExecution
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTaskDetailScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RecurringTaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // If task was deleted, navigate back
    if (!state.isLoading && state.task == null) {
        onNavigateBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.task?.title ?: "Task Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEdited) {
                        TextButton(onClick = { viewModel.processIntent(RecurringTaskDetailIntent.Save) }) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val task = state.task ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            item {
                OutlinedTextField(
                    value = state.editTitle,
                    onValueChange = { viewModel.processIntent(RecurringTaskDetailIntent.UpdateTitle(it)) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Prompt
            item {
                OutlinedTextField(
                    value = state.editPrompt,
                    onValueChange = { viewModel.processIntent(RecurringTaskDetailIntent.UpdatePrompt(it)) },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }

            // Schedule — time
            item {
                ScheduleSection(
                    hour = state.editHour,
                    minute = state.editMinute,
                    daysOfWeek = state.editDaysOfWeek,
                    onHourChange = { viewModel.processIntent(RecurringTaskDetailIntent.UpdateHour(it)) },
                    onMinuteChange = { viewModel.processIntent(RecurringTaskDetailIntent.UpdateMinute(it)) },
                    onToggleDay = { viewModel.processIntent(RecurringTaskDetailIntent.ToggleDay(it)) }
                )
            }

            // Related apps (display only)
            if (task.relatedApps.isNotEmpty()) {
                item {
                    Text("Related Apps", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = task.relatedApps.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Enable/Disable
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enabled", modifier = Modifier.weight(1f))
                    Switch(
                        checked = task.enabled,
                        onCheckedChange = { viewModel.processIntent(RecurringTaskDetailIntent.ToggleEnabled(it)) }
                    )
                }
            }

            // Run Now
            item {
                Button(
                    onClick = { viewModel.processIntent(RecurringTaskDetailIntent.RunNow) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRunning
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp).width(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Running...")
                    } else {
                        Text("Run Now")
                    }
                }
            }

            // Execution History
            if (state.executions.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text("Execution History", style = MaterialTheme.typography.titleSmall)
                }
                items(state.executions, key = { it.id }) { execution ->
                    ExecutionCard(execution)
                }
            }

            // Delete
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { viewModel.processIntent(RecurringTaskDetailIntent.ShowDeleteDialog) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Task")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Delete confirmation dialog
    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.processIntent(RecurringTaskDetailIntent.DismissDeleteDialog) },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${state.task?.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.processIntent(RecurringTaskDetailIntent.ConfirmDelete) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.processIntent(RecurringTaskDetailIntent.DismissDeleteDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleSection(
    hour: Int,
    minute: Int,
    daysOfWeek: List<Int>,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onToggleDay: (Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = false
    )

    Column {
        Text("Schedule", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        // Time display + picker trigger
        OutlinedCard(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(
                        "%02d:%02d %s",
                        if (hour % 12 == 0) 12 else hour % 12,
                        minute,
                        if (hour < 12) "AM" else "PM"
                    ),
                    style = MaterialTheme.typography.headlineMedium
                )
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Pick time"
                )
            }
        }

        // Time picker dialog
        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onHourChange(timePickerState.hour)
                        onMinuteChange(timePickerState.minute)
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Days of week (empty = every day)", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))

        val dayNames = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dayNames.forEach { (day, name) ->
                FilterChip(
                    selected = daysOfWeek.contains(day),
                    onClick = { onToggleDay(day) },
                    label = { Text(name) }
                )
            }
        }
    }
}

@Composable
private fun ExecutionCard(execution: TaskExecution) {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val statusColor = if (execution.status == "success")
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatter.format(Date(execution.executedAt)),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = execution.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
            if (execution.durationMs != null) {
                Text(
                    text = "${execution.durationMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!execution.summary.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = execution.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }
        }
    }
}
