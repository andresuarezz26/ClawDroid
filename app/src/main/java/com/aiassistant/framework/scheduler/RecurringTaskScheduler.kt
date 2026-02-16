package com.aiassistant.framework.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aiassistant.domain.model.RecurringTask
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RecurringTaskScheduler"

@Singleton
class RecurringTaskScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = WorkManager.getInstance(context)

    fun scheduleTask(task: RecurringTask) {
        if (task.isTimeSensitive) {
            scheduleExact(task)
        } else {
            scheduleFlexible(task)
        }
    }

    fun cancelTask(task: RecurringTask) {
        if (task.isTimeSensitive) {
            alarmManager.cancel(createAlarmPendingIntent(task.id))
        }
        workManager.cancelUniqueWork(workName(task.id))
        Log.i(TAG, "Cancelled task ${task.id} (${task.title})")
    }

    fun enqueueOneTimeWork(taskId: Long) {
        val data = Data.Builder().putLong(RecurringTaskWorker.KEY_TASK_ID, taskId).build()
        val request = OneTimeWorkRequestBuilder<RecurringTaskWorker>()
            .setInputData(data)
            .build()
        workManager.enqueue(request)
        Log.i(TAG, "Enqueued one-time work for task $taskId")
    }

    private fun scheduleExact(task: RecurringTask) {
        val nextFire = calculateNextFireTime(task.hour, task.minute, task.daysOfWeek)
        val pendingIntent = createAlarmPendingIntent(task.id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms, falling back to flexible for task ${task.id}")
            scheduleFlexible(task)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextFire,
            pendingIntent
        )
        Log.i(TAG, "Scheduled exact alarm for task ${task.id} at $nextFire")
    }

    private fun scheduleFlexible(task: RecurringTask) {
        val data = Data.Builder().putLong(RecurringTaskWorker.KEY_TASK_ID, task.id).build()
        val initialDelay = calculateNextFireTime(task.hour, task.minute, task.daysOfWeek) - System.currentTimeMillis()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<RecurringTaskWorker>(24, TimeUnit.HOURS)
            .setInputData(data)
            .setInitialDelay(initialDelay.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            workName(task.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.i(TAG, "Scheduled flexible periodic work for task ${task.id}, initial delay=${initialDelay}ms")
    }

    private fun createAlarmPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, RecurringTaskReceiver::class.java).apply {
            action = ACTION_RECURRING_TASK
            putExtra(RecurringTaskWorker.KEY_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_RECURRING_TASK = "com.aiassistant.ACTION_RECURRING_TASK"

        fun workName(taskId: Long) = "recurring_task_$taskId"

        fun calculateNextFireTime(hour: Int, minute: Int, daysOfWeek: List<Int>): Long {
            val now = Calendar.getInstance()
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If the time has already passed today, start from tomorrow
            if (!candidate.after(now)) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (daysOfWeek.isEmpty()) {
                return candidate.timeInMillis
            }

            // Find next valid day of week
            // Calendar: Sunday=1, Monday=2, ..., Saturday=7
            // Our format: Monday=1, ..., Sunday=7
            // Mapping: our 1(Mon)->Calendar 2, our 7(Sun)->Calendar 1
            for (i in 0 until 7) {
                val calDow = candidate.get(Calendar.DAY_OF_WEEK)
                val ourDow = if (calDow == Calendar.SUNDAY) 7 else calDow - 1
                if (daysOfWeek.contains(ourDow)) {
                    return candidate.timeInMillis
                }
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Fallback (shouldn't reach here if daysOfWeek is valid)
            return candidate.timeInMillis
        }
    }
}
