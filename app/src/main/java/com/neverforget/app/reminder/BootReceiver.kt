package com.neverforget.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neverforget.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val enabled = ReminderScheduler.isReminderEnabled(context)
            if (enabled) {
                val (hour, minute) = ReminderScheduler.getReminderTime(context)
                ReminderScheduler.scheduleDailyReminder(context, hour, minute)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val today = LocalDate.now()

                    val tasks = database.taskDao().getAllTasksSync()
                    tasks.filter { it.reminderHour != null && (it.recurrence.isRecurring || !it.isCompleted) }
                        .forEach { task ->
                            ReminderScheduler.scheduleTaskReminder(context, task)
                        }

                    val pills = database.pillDao().getAllPillsSync()
                    pills.filter { !it.isCourseCompleted(today) }
                        .forEach { pill ->
                            ReminderScheduler.schedulePillReminders(context, pill)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
