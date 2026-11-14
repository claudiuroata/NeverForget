package com.neverforget.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neverforget.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_ALARM = "com.neverforget.app.ACTION_TASK_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.neverforget.app.ACTION_SNOOZE_ALARM"
        const val ACTION_DAILY_SUMMARY_ALARM = "com.neverforget.app.ACTION_DAILY_SUMMARY_ALARM"
        const val ACTION_PILL_ALARM = "com.neverforget.app.ACTION_PILL_ALARM"
        const val ACTION_PILL_SNOOZE_ALARM = "com.neverforget.app.ACTION_PILL_SNOOZE_ALARM"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_PILL_ID = "pill_id"
        const val EXTRA_PILL_TIME_SLOT = "pill_time_slot"
        const val EXTRA_PILL_SLOT_INDEX = "pill_slot_index"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val today = LocalDate.now()
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

                when (action) {
                    ACTION_DAILY_SUMMARY_ALARM -> {
                        val (hour, minute) = ReminderScheduler.getReminderTime(context)
                        val allTasks = database.taskDao().getAllTasksSync()

                        val overdueTasks = allTasks.filter { it.isOverdue(today) }
                        val todayPendingTasks = allTasks.filter { it.isPendingOn(today) && !it.isOverdue(today) }
                        val upcomingTasks = allTasks.filter {
                            it.isPendingInNext7Days(today) && !it.isPendingOn(today) && !it.isOverdue(today)
                        }

                        if (overdueTasks.isNotEmpty() || todayPendingTasks.isNotEmpty() || upcomingTasks.isNotEmpty()) {
                            NotificationHelper.showPendingTasksNotification(
                                context,
                                todayTasks = todayPendingTasks,
                                upcomingTasks = upcomingTasks,
                                overdueTasks = overdueTasks
                            )
                        }

                        // Re-arm exact alarm for tomorrow at the same time
                        ReminderScheduler.scheduleDailyReminder(context, hour, minute)
                    }
                    ACTION_TASK_ALARM -> {
                        if (taskId != -1L) {
                            val task = database.taskDao().getTaskById(taskId)
                            if (task != null && task.isPendingOn(today)) {
                                NotificationHelper.showTaskSpecificNotification(context, task)
                            }
                        }
                    }
                    ACTION_SNOOZE_ALARM -> {
                        if (taskId != -1L) {
                            val task = database.taskDao().getTaskById(taskId)
                            if (task != null && task.isPendingOn(today)) {
                                NotificationHelper.showTaskSpecificNotification(context, task)
                            }
                        } else {
                            val allTasks = database.taskDao().getAllTasksSync()
                            val overdueTasks = allTasks.filter { it.isOverdue(today) }
                            val todayTasks = allTasks.filter { it.isPendingOn(today) && !it.isOverdue(today) }
                            val upcomingTasks = allTasks.filter {
                                it.isPendingInNext7Days(today) && !it.isPendingOn(today) && !it.isOverdue(today)
                            }

                            if (overdueTasks.isNotEmpty() || todayTasks.isNotEmpty() || upcomingTasks.isNotEmpty()) {
                                NotificationHelper.showPendingTasksNotification(
                                    context,
                                    todayTasks = todayTasks,
                                    upcomingTasks = upcomingTasks,
                                    overdueTasks = overdueTasks
                                )
                            }
                        }
                    }
                    ACTION_PILL_ALARM -> {
                        val pillId = intent.getLongExtra(EXTRA_PILL_ID, -1L)
                        val timeSlot = intent.getStringExtra(EXTRA_PILL_TIME_SLOT)
                        if (pillId != -1L && !timeSlot.isNullOrBlank()) {
                            val pill = database.pillDao().getPillById(pillId)
                            if (pill != null) {
                                if (pill.isActiveOn(today) && !pill.isDoseTaken(today, timeSlot)) {
                                    NotificationHelper.showPillDoseNotification(context, pill, timeSlot)
                                }
                                // Re-arm next scheduled alarm
                                ReminderScheduler.schedulePillReminders(context, pill)
                            }
                        }
                    }
                    ACTION_PILL_SNOOZE_ALARM -> {
                        val pillId = intent.getLongExtra(EXTRA_PILL_ID, -1L)
                        val timeSlot = intent.getStringExtra(EXTRA_PILL_TIME_SLOT)
                        if (pillId != -1L && !timeSlot.isNullOrBlank()) {
                            val pill = database.pillDao().getPillById(pillId)
                            if (pill != null && pill.isActiveOn(today) && !pill.isDoseTaken(today, timeSlot)) {
                                NotificationHelper.showPillDoseNotification(context, pill, timeSlot)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
