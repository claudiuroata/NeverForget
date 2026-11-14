package com.neverforget.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(tableName = "pills")
data class Pill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String = "1 pill",
    val notes: String = "",
    val mealContext: MealContext = MealContext.ANYTIME,
    val doseTimes: String = "08:00", // comma-separated "HH:mm" strings sorted chronologically
    val startDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val durationDays: Int? = null, // null = ongoing; > 0 = fixed course of N days
    val takenHistory: String = "", // semicolon-separated "YYYY-MM-DD@HH:mm" entries
    val snoozedSlotKey: String? = null, // e.g. "2026-11-14@08:00"
    val snoozedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private const val MAX_HISTORY_ENTRIES = 120

        fun buildSlotKey(date: LocalDate, timeSlot: String): String {
            return "${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}@$timeSlot"
        }
    }

    /**
     * Returns scheduled daily dose times sorted chronologically (e.g. ["08:00", "20:00"]).
     */
    fun getScheduledTimes(): List<String> {
        return doseTimes.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getStartDate(): LocalDate = LocalDate.ofEpochDay(startDateEpochDay)

    /**
     * Checks whether this medication course is active on the specified date.
     */
    fun isActiveOn(date: LocalDate): Boolean {
        if (date.isBefore(getStartDate())) return false
        val duration = durationDays
        if (duration == null || duration <= 0) return true
        val diff = date.toEpochDay() - startDateEpochDay
        return diff in 0 until duration.toLong()
    }

    /**
     * Checks if a fixed-duration medication course has finished.
     */
    fun isCourseCompleted(today: LocalDate = LocalDate.now()): Boolean {
        val duration = durationDays
        if (duration == null || duration <= 0) return false
        val daysElapsed = today.toEpochDay() - startDateEpochDay
        if (daysElapsed >= duration) return true
        val times = getScheduledTimes()
        return daysElapsed == (duration - 1).toLong() &&
            times.isNotEmpty() &&
            times.all { isDoseTaken(today, it) }
    }

    /**
     * Formats course duration status (e.g. "Ongoing", "Day 4 of 10", or "Finished (10 days)").
     */
    fun getCourseProgressText(today: LocalDate = LocalDate.now()): String {
        val duration = durationDays
        if (duration == null || duration <= 0) return "Ongoing"
        if (today.toEpochDay() >= startDateEpochDay + duration) {
            return "Finished ($duration days)"
        }
        val currentDay = (today.toEpochDay() - startDateEpochDay + 1).coerceAtLeast(1)
        return "Day $currentDay of $duration"
    }

    private fun parseHistoryList(): List<String> {
        if (takenHistory.isBlank()) return emptyList()
        return takenHistory.split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Returns true if the dose for (date, timeSlot) was marked as taken.
     */
    fun isDoseTaken(date: LocalDate, timeSlot: String): Boolean {
        val key = buildSlotKey(date, timeSlot)
        return key in parseHistoryList()
    }

    /**
     * Toggles whether the dose for (date, timeSlot) was taken and clears matching snooze.
     */
    fun toggleDoseTaken(date: LocalDate, timeSlot: String): Pill {
        val key = buildSlotKey(date, timeSlot)
        val entries = parseHistoryList().toMutableList()
        if (key in entries) {
            entries.removeAll { it == key }
        } else {
            entries.add(key)
        }
        val trimmed = entries.takeLast(MAX_HISTORY_ENTRIES)
        val clearSnooze = snoozedSlotKey == key
        return copy(
            takenHistory = trimmed.joinToString(";"),
            snoozedSlotKey = if (clearSnooze) null else snoozedSlotKey,
            snoozedUntil = if (clearSnooze) null else snoozedUntil
        )
    }

    /**
     * Idempotently marks the dose for (date, timeSlot) as taken and clears matching snooze.
     */
    fun markDoseTaken(date: LocalDate, timeSlot: String): Pill {
        val key = buildSlotKey(date, timeSlot)
        val entries = parseHistoryList().toMutableList()
        if (key !in entries) {
            entries.add(key)
        }
        val trimmed = entries.takeLast(MAX_HISTORY_ENTRIES)
        val clearSnooze = snoozedSlotKey == key
        return copy(
            takenHistory = trimmed.joinToString(";"),
            snoozedSlotKey = if (clearSnooze) null else snoozedSlotKey,
            snoozedUntil = if (clearSnooze) null else snoozedUntil
        )
    }

    /**
     * Formats snooze status string if the specific slot is currently snoozed.
     */
    fun getFormattedSnoozeText(today: LocalDate, timeSlot: String): String? {
        val key = buildSlotKey(today, timeSlot)
        val until = snoozedUntil ?: return null
        if (snoozedSlotKey != key || until <= System.currentTimeMillis()) return null
        val localTime = Instant.ofEpochMilli(until)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return String.format(
            Locale.getDefault(),
            "Snoozed until %02d:%02d",
            localTime.hour,
            localTime.minute
        )
    }
}
