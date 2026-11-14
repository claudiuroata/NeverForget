package com.neverforget.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.neverforget.app.MainActivity
import com.neverforget.app.R
import com.neverforget.app.data.Pill
import com.neverforget.app.data.Task

object NotificationHelper {

    const val CHANNEL_ID = "daily_todo_reminders"
    const val NOTIFICATION_ID = 1001
    const val PILL_CHANNEL_ID = "pill_reminders_channel"
    const val PILL_NOTIFICATION_BASE_ID = 20000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)

            val pillName = context.getString(R.string.pill_channel_name)
            val pillDesc = context.getString(R.string.pill_channel_description)
            val pillChannel = NotificationChannel(PILL_CHANNEL_ID, pillName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = pillDesc
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(pillChannel)
        }
    }

    fun showPendingTasksNotification(
        context: Context,
        todayTasks: List<Task>,
        upcomingTasks: List<Task> = emptyList(),
        overdueTasks: List<Task> = emptyList()
    ) {
        if (todayTasks.isEmpty() && upcomingTasks.isEmpty() && overdueTasks.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val todayCount = todayTasks.size
        val upcomingCount = upcomingTasks.size
        val overdueCount = overdueTasks.size

        val title = when {
            overdueCount > 0 -> "NeverForget: ⚠️ $overdueCount Overdue! $todayCount Today, $upcomingCount Upcoming"
            todayCount > 0 -> "NeverForget: $todayCount task${if (todayCount > 1) "s" else ""} pending today"
            else -> "NeverForget: $upcomingCount task${if (upcomingCount > 1) "s" else ""} upcoming this week"
        }

        val collapsedText = when {
            overdueTasks.isNotEmpty() -> "⚠️ OVERDUE: " + overdueTasks.joinToString(", ") { it.title }
            todayTasks.isNotEmpty() -> "Today: " + todayTasks.joinToString(", ") { it.title }
            else -> "Upcoming: " + upcomingTasks.joinToString(", ") { it.title }
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
        val builderText = StringBuilder()

        if (overdueTasks.isNotEmpty()) {
            builderText.append("⚠️ OVERDUE / MISSED:\n")
            overdueTasks.take(4).forEach { task ->
                val timeBadge = task.getFormattedTimeText()?.let { " ($it)" } ?: ""
                builderText.append("• ").append(task.title).append(timeBadge).append("\n")
            }
            if (overdueTasks.size > 4) {
                builderText.append("...and ").append(overdueTasks.size - 4).append(" more overdue\n")
            }
            builderText.append("\n")
        }

        if (todayTasks.isNotEmpty()) {
            builderText.append("☀️ TODAY:\n")
            todayTasks.take(4).forEach { task ->
                val timeBadge = task.getFormattedTimeText()?.let { " ($it)" } ?: ""
                builderText.append("• ").append(task.title).append(timeBadge).append("\n")
            }
            if (todayTasks.size > 4) {
                builderText.append("...and ").append(todayTasks.size - 4).append(" more\n")
            }
            builderText.append("\n")
        }

        if (upcomingTasks.isNotEmpty()) {
            builderText.append("📅 NEXT 7 DAYS:\n")
            upcomingTasks.take(4).forEach { task ->
                val timeBadge = task.getFormattedTimeText()?.let { " ($it)" } ?: ""
                val recurrenceBadge = if (task.recurrence.isRecurring) " [${task.getFormattedRecurrenceText()}]" else ""
                builderText.append("• ").append(task.title).append(timeBadge).append(recurrenceBadge).append("\n")
            }
            if (upcomingTasks.size > 4) {
                builderText.append("...and ").append(upcomingTasks.size - 4).append(" more\n")
            }
        }

        builderText.append("\n(Tap to view • Expand to snooze)")

        bigTextStyle.bigText(builderText.toString().trimEnd())
        bigTextStyle.setSummaryText("$overdueCount overdue • $todayCount today • $upcomingCount upcoming")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", -1L)
            putExtra("open_pending_tab", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_todo)
            .setContentTitle(title)
            .setContentText(collapsedText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(createSnoozePendingIntent(context, 1, NOTIFICATION_ID, -1L))
            .addAction(0, "Snooze 1h", createSnoozePendingIntent(context, 1, NOTIFICATION_ID, -1L))
            .addAction(0, "Snooze 3h", createSnoozePendingIntent(context, 3, NOTIFICATION_ID, -1L))
            .addAction(0, "Snooze 6h", createSnoozePendingIntent(context, 6, NOTIFICATION_ID, -1L))
            .addAction(0, "Snooze 1d", createSnoozePendingIntent(context, 24, NOTIFICATION_ID, -1L))
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showTestNotification(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val testNotifId = NOTIFICATION_ID + 1

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", -1L)
            putExtra("open_task_detail", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val expandedMessage = "$message\n\n(Expand or swipe down on notification to see Snooze actions)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_todo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedMessage))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Snooze 1h", createSnoozePendingIntent(context, 1, testNotifId))
            .addAction(0, "Snooze 3h", createSnoozePendingIntent(context, 3, testNotifId))
            .addAction(0, "Snooze 6h", createSnoozePendingIntent(context, 6, testNotifId))
            .addAction(0, "Snooze 1d", createSnoozePendingIntent(context, 24, testNotifId))
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(testNotifId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showTaskSpecificNotification(context: Context, task: Task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val taskNotifId = (NOTIFICATION_ID + 2 + task.id).toInt()
        val timeText = task.getFormattedTimeText() ?: ""
        val title = "Task Reminder: ${task.title}"
        val bodyText = if (task.description.isNotBlank()) {
            "${task.description} ($timeText)"
        } else {
            "Scheduled for $timeText"
        }
        val expandedText = "$bodyText\n\n(Tap to mark done / snooze)"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", task.id)
            putExtra("open_task_detail", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_todo)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(createSnoozePendingIntent(context, 1, taskNotifId, task.id))
            .addAction(0, "Snooze 1h", createSnoozePendingIntent(context, 1, taskNotifId, task.id))
            .addAction(0, "Snooze 3h", createSnoozePendingIntent(context, 3, taskNotifId, task.id))
            .addAction(0, "Snooze 6h", createSnoozePendingIntent(context, 6, taskNotifId, task.id))
            .addAction(0, "Snooze 1d", createSnoozePendingIntent(context, 24, taskNotifId, task.id))
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(taskNotifId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun createSnoozePendingIntent(
        context: Context,
        hours: Int,
        notificationId: Int,
        taskId: Long = -1L
    ): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_SNOOZE
            putExtra(SnoozeReceiver.EXTRA_SNOOZE_HOURS, hours)
            putExtra(SnoozeReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SnoozeReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            (notificationId * 100) + (if (taskId != -1L) taskId.toInt() * 10 else 0) + hours,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showPillDoseNotification(context: Context, pill: Pill, timeSlot: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val slotIndex = pill.getScheduledTimes().indexOf(timeSlot).let { idx ->
            if (idx >= 0) idx else (timeSlot.hashCode() and 0x0F)
        }
        val pillNotifId = PILL_NOTIFICATION_BASE_ID + (pill.id.toInt() * 20) + slotIndex

        val title = "💊 ${pill.name} (${pill.dosage})"
        val summaryLine = "$timeSlot dose • ${pill.mealContext.displayName} • ${pill.getCourseProgressText()}"
        val expandedText = if (pill.notes.isNotBlank()) {
            "$summaryLine\n${pill.notes}"
        } else {
            summaryLine
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_pills_tab", true)
            putExtra("pill_id", pill.id)
            putExtra("pill_time_slot", timeSlot)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            pillNotifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenPendingIntent = createPillTakenPendingIntent(context, pill.id, timeSlot, pillNotifId)
        val snooze15PendingIntent = createPillSnoozePendingIntent(context, pill.id, timeSlot, 15, pillNotifId)
        val snooze30PendingIntent = createPillSnoozePendingIntent(context, pill.id, timeSlot, 30, pillNotifId)

        val notification = NotificationCompat.Builder(context, PILL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pill)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "✓ Taken", takenPendingIntent)
            .addAction(0, "Snooze 15m", snooze15PendingIntent)
            .addAction(0, "Snooze 30m", snooze30PendingIntent)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(pillNotifId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun createPillTakenPendingIntent(
        context: Context,
        pillId: Long,
        timeSlot: String,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, PillActionReceiver::class.java).apply {
            action = PillActionReceiver.ACTION_MARK_PILL_TAKEN
            putExtra(PillActionReceiver.EXTRA_PILL_ID, pillId)
            putExtra(PillActionReceiver.EXTRA_TIME_SLOT, timeSlot)
            putExtra(PillActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPillSnoozePendingIntent(
        context: Context,
        pillId: Long,
        timeSlot: String,
        minutes: Int,
        notificationId: Int
    ): PendingIntent {
        val intent = Intent(context, PillActionReceiver::class.java).apply {
            action = PillActionReceiver.ACTION_SNOOZE_PILL
            putExtra(PillActionReceiver.EXTRA_PILL_ID, pillId)
            putExtra(PillActionReceiver.EXTRA_TIME_SLOT, timeSlot)
            putExtra(PillActionReceiver.EXTRA_SNOOZE_MINUTES, minutes)
            putExtra(PillActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val offset = if (minutes == 15) 2 else 3
        return PendingIntent.getBroadcast(
            context,
            notificationId * 10 + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
