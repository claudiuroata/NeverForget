package com.neverforget.app

import android.app.Application
import com.neverforget.app.data.AppDatabase
import com.neverforget.app.data.PillRepository
import com.neverforget.app.data.TaskRepository
import com.neverforget.app.reminder.NotificationHelper
import com.neverforget.app.reminder.ReminderScheduler

class TodoApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TaskRepository(database.taskDao()) }
    val pillRepository by lazy { PillRepository(database.pillDao()) }

    override fun onCreate() {
        super.onCreate()

        // Create the notification channel
        NotificationHelper.createNotificationChannel(this)

        // Ensure daily reminder worker is scheduled if enabled
        if (ReminderScheduler.isReminderEnabled(this)) {
            val (hour, minute) = ReminderScheduler.getReminderTime(this)
            ReminderScheduler.scheduleDailyReminder(this, hour, minute)
        }
    }
}
