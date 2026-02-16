package com.aiassistant.framework.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "RecurringTaskReceiver"

class RecurringTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(RecurringTaskWorker.KEY_TASK_ID, -1L)
        if (taskId == -1L) {
            Log.w(TAG, "Received alarm with no task ID")
            return
        }

        Log.i(TAG, "Alarm fired for task $taskId, enqueuing worker")
        RecurringTaskScheduler(context).enqueueOneTimeWork(taskId)
    }
}
