package com.neverforget.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.work.*
import com.neverforget.app.data.Pill
import com.neverforget.app.data.Task
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val PREFS_NAME = "todo_reminder_prefs"
    private const val KEY_REMINDER_ENABLED = "reminder_enabled"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"

    const val WORK_NAME_DAILY_REMINDER = "daily_task_reminder_work"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMINDER_ENABLED, true)
    }

    fun getReminderTime(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val hour = prefs.getInt(KEY_REMINDER_HOUR, 9) // Default 9:00 AM
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun saveReminderSettings(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()

        if (enabled) {
            scheduleDailyReminder(context, hour, minute)
        } else {
            cancelDailyReminder(context)
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DAILY_SUMMARY_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            88888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DAILY_SUMMARY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            88888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Schedules an exact alarm for a specific task using AlarmManager.
     * Guarantees exact delivery at task.reminderHour:task.reminderMinute without WorkManager delay.
     */
    fun scheduleTaskReminder(context: Context, task: Task) {
        val hour = task.reminderHour ?: return
        val minute = task.reminderMinute ?: 0

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TASK_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    /**
     * Cancels exact scheduled alarm for a specific task.
     */
    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TASK_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Schedules exact snooze alarm for durationHours.
     */
    fun scheduleExactSnooze(context: Context, durationHours: Int, taskId: Long = -1L) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + (durationHours.toLong() * 3600 * 1000)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }

        val requestCode = if (taskId != -1L) (99000 + taskId).toInt() else 99999

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    private fun scheduleExactAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Schedules exact alarms for every daily time slot of a medication pill.
     * Uses requestCode range 200000+ so it never collides with task alarms.
     */
    fun schedulePillReminders(context: Context, pill: Pill) {
        cancelPillReminders(context, pill.id)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nowMillis = System.currentTimeMillis()
        val today = LocalDate.now()
        val startDate = pill.getStartDate()

        pill.getScheduledTimes().take(16).forEachIndexed { index, timeSlot ->
            val parts = timeSlot.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@forEachIndexed
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= nowMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            var triggerDate = Instant.ofEpochMilli(calendar.timeInMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            if (triggerDate.isBefore(startDate)) {
                calendar.set(startDate.year, startDate.monthValue - 1, startDate.dayOfMonth, hour, minute, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                triggerDate = startDate
            }

            if (triggerDate == today && pill.isDoseTaken(today, timeSlot)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                triggerDate = triggerDate.plusDays(1)
            }

            if (!pill.isActiveOn(triggerDate)) {
                return@forEachIndexed
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_PILL_ALARM
                putExtra(AlarmReceiver.EXTRA_PILL_ID, pill.id)
                putExtra(AlarmReceiver.EXTRA_PILL_TIME_SLOT, timeSlot)
                putExtra(AlarmReceiver.EXTRA_PILL_SLOT_INDEX, index)
            }

            val requestCode = 200000 + (pill.id.toInt() * 20) + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
        }
    }

    /**
     * Cancels all scheduled slot alarms (indices 0..15) for a medication pill.
     */
    fun cancelPillReminders(context: Context, pillId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..15) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_PILL_ALARM
            }
            val requestCode = 200000 + (pillId.toInt() * 20) + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Schedules an exact short-interval snooze alarm for a medication dose.
     * Uses requestCode range 300000+ isolated from task alarms.
     */
    fun schedulePillSnooze(context: Context, pillId: Long, timeSlot: String, durationMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + (durationMinutes.toLong() * 60_000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PILL_SNOOZE_ALARM
            putExtra(AlarmReceiver.EXTRA_PILL_ID, pillId)
            putExtra(AlarmReceiver.EXTRA_PILL_TIME_SLOT, timeSlot)
        }

        val requestCode = 300000 + (pillId.toInt() * 100) + (timeSlot.hashCode() and 0x3F)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    /**
     * Executes an immediate check/reminder for testing.
     */
    fun triggerImmediateCheck(context: Context) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(oneTimeRequest)
    }
}
