package com.neverforget.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.neverforget.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SnoozeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "com.neverforget.app.ACTION_SNOOZE"
        const val EXTRA_SNOOZE_HOURS = "snooze_hours"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TASK_ID = "task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE) {
            val hours = intent.getIntExtra(EXTRA_SNOOZE_HOURS, 1)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NotificationHelper.NOTIFICATION_ID)
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

            val snoozeTimeMillis = System.currentTimeMillis() + (hours.toLong() * 3600 * 1000)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val taskDao = database.taskDao()

                    if (taskId != -1L) {
                        val task = taskDao.getTaskById(taskId)
                        if (task != null) {
                            taskDao.updateTask(task.copy(snoozedUntil = snoozeTimeMillis))
                        }
                    } else {
                        // Snooze all pending tasks for today
                        val today = LocalDate.now()
                        val pending = taskDao.getAllTasksSync().filter { it.isPendingOn(today) }
                        pending.forEach { task ->
                            taskDao.updateTask(task.copy(snoozedUntil = snoozeTimeMillis))
                        }
                    }

                    // Schedule exact snoozed alarm
                    ReminderScheduler.scheduleExactSnooze(context, hours, taskId)

                    // Dismiss the active notification
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.cancel(notificationId)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }

            // Display toast feedback to the user
            val label = if (hours == 24) "1 day" else "$hours hour${if (hours > 1) "s" else ""}"
            Toast.makeText(
                context,
                "Notification snoozed for $label",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
