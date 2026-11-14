package com.neverforget.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.neverforget.app.data.AppDatabase
import com.neverforget.app.data.Pill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class PillActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_PILL_TAKEN = "com.neverforget.app.ACTION_MARK_PILL_TAKEN"
        const val ACTION_SNOOZE_PILL = "com.neverforget.app.ACTION_SNOOZE_PILL"
        const val EXTRA_PILL_ID = "pill_id"
        const val EXTRA_TIME_SLOT = "pill_time_slot"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pillId = intent.getLongExtra(EXTRA_PILL_ID, -1L)
        val timeSlot = intent.getStringExtra(EXTRA_TIME_SLOT) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NotificationHelper.PILL_NOTIFICATION_BASE_ID)

        if (pillId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val pillDao = database.pillDao()
                val pill = pillDao.getPillById(pillId)

                when (action) {
                    ACTION_MARK_PILL_TAKEN -> {
                        if (pill != null) {
                            val updated = pill.markDoseTaken(LocalDate.now(), timeSlot)
                            pillDao.updatePill(updated)
                            ReminderScheduler.schedulePillReminders(context, updated)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Marked ${pill.name} ($timeSlot) as taken ✓",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                    ACTION_SNOOZE_PILL -> {
                        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)
                        val snoozeUntil = System.currentTimeMillis() + (minutes.toLong() * 60_000L)
                        if (pill != null) {
                            val slotKey = Pill.buildSlotKey(LocalDate.now(), timeSlot)
                            pillDao.updatePill(
                                pill.copy(
                                    snoozedSlotKey = slotKey,
                                    snoozedUntil = snoozeUntil
                                )
                            )
                        }
                        ReminderScheduler.schedulePillSnooze(context, pillId, timeSlot, minutes)
                        NotificationManagerCompat.from(context).cancel(notificationId)

                        val label = if (minutes >= 60) "${minutes / 60}h" else "${minutes}m"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Pill reminder snoozed for $label",
                                Toast.LENGTH_SHORT
                            ).show()
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
