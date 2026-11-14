package com.neverforget.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceDaysOfWeek: String = "", // e.g. "MONDAY,WEDNESDAY,FRIDAY"
    val recurrenceMonthDay: Int? = null,   // e.g. 1..31
    val recurrenceStartMonth: Int? = null, // e.g. 1..12 (Jan..Dec)
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val lastCompletedDate: String? = null, // "YYYY-MM-DD"
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val snoozedUntil: Long? = null        // Epoch timestamp in millis when snooze expires
) {
    /**
     * Helper to parse selected days of week for weekly recurrence.
     */
    fun getSelectedDaysOfWeek(): Set<DayOfWeek> {
        if (recurrenceDaysOfWeek.isBlank()) return emptySet()
        return recurrenceDaysOfWeek.split(",")
            .mapNotNull {
                try { DayOfWeek.valueOf(it.trim()) } catch (e: Exception) { null }
            }.toSet()
    }

    /**
     * Formats reminder time string (e.g. "14:30").
     */
    fun getFormattedTimeText(): String? {
        val hour = reminderHour ?: return null
        val minute = reminderMinute ?: 0
        return String.format(
            java.util.Locale.getDefault(),
            "%02d:%02d",
            hour,
            minute
        )
    }

    /**
     * Formats snooze status string if the task is currently snoozed.
     */
    fun getFormattedSnoozeText(): String? {
        val until = snoozedUntil ?: return null
        if (until <= System.currentTimeMillis()) return null
        val localTime = Instant.ofEpochMilli(until)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return String.format(
            java.util.Locale.getDefault(),
            "Snoozed until %02d:%02d %s",
            if (localTime.hour % 12 == 0) 12 else localTime.hour % 12,
            localTime.minute,
            if (localTime.hour >= 12) "PM" else "AM"
        )
    }

    /**
     * Formats recurrence description string for badges and UI.
     */
    fun getFormattedRecurrenceText(): String {
        return when (recurrence) {
            RecurrenceType.NONE -> "Once"
            RecurrenceType.DAILY -> "Daily"
            RecurrenceType.WEEKDAYS -> "Weekdays"
            RecurrenceType.WEEKLY -> {
                val days = getSelectedDaysOfWeek()
                if (days.isEmpty()) {
                    "Weekly"
                } else if (days.size == 7) {
                    "Every day"
                } else if (days == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
                    "Weekdays"
                } else {
                    val shortNames = days.sortedBy { it.value }.joinToString(", ") {
                        it.name.take(3).lowercase().replaceFirstChar { char -> char.uppercase() }
                    }
                    shortNames
                }
            }
            RecurrenceType.MONTHLY -> {
                val day = recurrenceMonthDay
                if (day == null) "Monthly" else "Monthly (${day}${getOrdinalSuffix(day)})"
            }
            RecurrenceType.QUARTERLY -> {
                val day = recurrenceMonthDay
                val startM = recurrenceStartMonth
                if (startM != null && day != null) {
                    val monthName = Month.of(startM).name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    "Quarterly ($monthName ${day}${getOrdinalSuffix(day)})"
                } else if (day != null) {
                    "Quarterly (${day}${getOrdinalSuffix(day)})"
                } else {
                    "Quarterly"
                }
            }
            RecurrenceType.YEARLY -> {
                val day = recurrenceMonthDay
                val startM = recurrenceStartMonth
                if (startM != null && day != null) {
                    val monthName = Month.of(startM).name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    "Yearly ($monthName ${day}${getOrdinalSuffix(day)})"
                } else if (day != null) {
                    "Yearly (${day}${getOrdinalSuffix(day)})"
                } else {
                    "Yearly"
                }
            }
        }
    }

    private fun getOrdinalSuffix(n: Int): String {
        return when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
    }

    /**
     * Checks if this task should be presented or is scheduled for the given date.
     */
    fun isDueOn(date: LocalDate): Boolean {
        return when (recurrence) {
            RecurrenceType.NONE -> true
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKDAYS -> {
                val dow = date.dayOfWeek
                dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
            }
            RecurrenceType.WEEKLY -> {
                val days = getSelectedDaysOfWeek()
                if (days.isEmpty()) {
                    val createdDate = try {
                        Instant.ofEpochMilli(createdAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    } catch (e: Exception) {
                        null
                    }
                    if (createdDate != null) {
                        date.dayOfWeek == createdDate.dayOfWeek
                    } else {
                        true
                    }
                } else {
                    date.dayOfWeek in days
                }
            }
            RecurrenceType.MONTHLY -> {
                val day = recurrenceMonthDay
                if (day != null) {
                    val targetDay = day.coerceAtMost(date.lengthOfMonth())
                    date.dayOfMonth == targetDay
                } else {
                    val createdDate = try {
                        Instant.ofEpochMilli(createdAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    } catch (e: Exception) {
                        null
                    }
                    if (createdDate != null) {
                        date.dayOfMonth == createdDate.dayOfMonth.coerceAtMost(date.lengthOfMonth())
                    } else {
                        true
                    }
                }
            }
            RecurrenceType.QUARTERLY -> {
                val createdDate = try {
                    Instant.ofEpochMilli(createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } catch (e: Exception) {
                    date
                }
                val startM = recurrenceStartMonth ?: createdDate.monthValue
                val day = recurrenceMonthDay ?: createdDate.dayOfMonth
                val targetDay = day.coerceAtMost(date.lengthOfMonth())

                val monthDiff = (date.year - createdDate.year) * 12 + (date.monthValue - startM)
                if (monthDiff >= 0 && monthDiff % 3 == 0) {
                    date.dayOfMonth == targetDay
                } else {
                    false
                }
            }
            RecurrenceType.YEARLY -> {
                val createdDate = try {
                    Instant.ofEpochMilli(createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } catch (e: Exception) {
                    date
                }
                val startM = recurrenceStartMonth ?: createdDate.monthValue
                val day = recurrenceMonthDay ?: createdDate.dayOfMonth
                val targetDay = day.coerceAtMost(date.lengthOfMonth())

                if (date.year >= createdDate.year && date.monthValue == startM) {
                    date.dayOfMonth == targetDay
                } else {
                    false
                }
            }
        }
    }

    /**
     * Calculates the next upcoming due date for this task on or after fromDate.
     */
    fun getNextDueDate(fromDate: LocalDate = LocalDate.now()): LocalDate {
        return when (recurrence) {
            RecurrenceType.NONE -> {
                val createdDate = try {
                    Instant.ofEpochMilli(createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } catch (e: Exception) {
                    fromDate
                }
                if (createdDate.isBefore(fromDate)) fromDate else createdDate
            }
            RecurrenceType.DAILY -> fromDate
            RecurrenceType.WEEKDAYS -> {
                var d = fromDate
                while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
                    d = d.plusDays(1)
                }
                d
            }
            RecurrenceType.WEEKLY -> {
                val days = getSelectedDaysOfWeek()
                if (days.isEmpty()) {
                    fromDate
                } else {
                    var d = fromDate
                    var count = 0
                    while (d.dayOfWeek !in days && count < 7) {
                        d = d.plusDays(1)
                        count++
                    }
                    d
                }
            }
            RecurrenceType.MONTHLY, RecurrenceType.QUARTERLY, RecurrenceType.YEARLY -> {
                var d = fromDate
                var count = 0
                while (!isDueOn(d) && count < 366) {
                    d = d.plusDays(1)
                    count++
                }
                d
            }
        }
    }

    /**
     * Checks if this task has a missed/uncompleted due date strictly in the past.
     */
    fun isOverdue(today: LocalDate = LocalDate.now()): Boolean {
        // If the task is already completed for today or completed overall (for one-time), it cannot be overdue today
        if (isCompletedOn(today)) return false
        if (recurrence == RecurrenceType.NONE && isCompleted) return false

        val createdDate = try {
            Instant.ofEpochMilli(createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } catch (e: Exception) {
            today
        }

        if (!createdDate.isBefore(today)) return false

        val completedDate = lastCompletedDate?.let {
            try {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                null
            }
        }

        return when (recurrence) {
            RecurrenceType.NONE -> {
                !isCompleted
            }
            RecurrenceType.DAILY -> {
                val yesterday = today.minusDays(1)
                if (yesterday < createdDate) {
                    false
                } else if (completedDate == null) {
                    true
                } else {
                    completedDate < yesterday
                }
            }
            RecurrenceType.WEEKDAYS -> {
                var d = today.minusDays(1)
                while (d >= createdDate && (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY)) {
                    d = d.minusDays(1)
                }
                if (d < createdDate || d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
                    false
                } else if (completedDate == null) {
                    true
                } else {
                    completedDate < d
                }
            }
            RecurrenceType.WEEKLY, RecurrenceType.MONTHLY, RecurrenceType.QUARTERLY, RecurrenceType.YEARLY -> {
                var d = today.minusDays(1)
                var foundDue: LocalDate? = null
                var count = 0
                while (d >= createdDate && count < 366) {
                    if (isDueOn(d)) {
                        foundDue = d
                        break
                    }
                    d = d.minusDays(1)
                    count++
                }
                if (foundDue == null) {
                    false
                } else if (completedDate == null) {
                    true
                } else {
                    completedDate < foundDue
                }
            }
        }
    }

    /**
     * Checks if the task was completed on the specified date.
     */
    fun isCompletedOn(date: LocalDate): Boolean {
        if (recurrence == RecurrenceType.NONE) {
            return isCompleted
        }

        if (lastCompletedDate == null) {
            return false
        }

        return try {
            val completedDate = LocalDate.parse(lastCompletedDate, DateTimeFormatter.ISO_LOCAL_DATE)
            completedDate == date
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns true if the task requires attention today.
     */
    fun isPendingOn(date: LocalDate = LocalDate.now()): Boolean {
        return isDueOn(date) && !isCompletedOn(date)
    }

    /**
     * Returns true if the task is pending on any day in the next 7 days.
     */
    fun isPendingInNext7Days(today: LocalDate = LocalDate.now()): Boolean {
        return (0L..6L).any { dayOffset ->
            isPendingOn(today.plusDays(dayOffset))
        }
    }

    /**
     * Toggles task completion for the given date.
     */
    fun toggleCompletion(date: LocalDate = LocalDate.now()): Task {
        val currentlyCompleted = isCompletedOn(date)
        return if (currentlyCompleted) {
            copy(
                isCompleted = false,
                lastCompletedDate = null,
                snoozedUntil = null
            )
        } else {
            copy(
                isCompleted = true,
                lastCompletedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                snoozedUntil = null
            )
        }
    }
}
