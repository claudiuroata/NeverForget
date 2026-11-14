package com.neverforget.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class TaskRecurrenceTest {

    @Test
    fun testOneTimeTask_DueCompletionAndToggle() {
        val today = LocalDate.of(2026, 11, 14)
        val task = Task(
            id = 1L,
            title = "Buy groceries",
            recurrence = RecurrenceType.NONE
        )

        assertTrue(task.isDueOn(today))
        assertTrue(task.isPendingOn(today))
        assertFalse(task.isCompletedOn(today))
        assertEquals("Once", task.getFormattedRecurrenceText())

        val completed = task.toggleCompletion(today)
        assertTrue(completed.isCompleted)
        assertTrue(completed.isCompletedOn(today))
        assertTrue(completed.isCompletedOn(today.plusDays(1)))
        assertFalse(completed.isPendingOn(today))

        val reopened = completed.toggleCompletion(today)
        assertFalse(reopened.isCompleted)
        assertNull(reopened.lastCompletedDate)
        assertTrue(reopened.isPendingOn(today))
    }

    @Test
    fun testDailyTask_AutoResetsNextDay() {
        val today = LocalDate.of(2026, 11, 14)
        val tomorrow = today.plusDays(1)
        val task = Task(
            id = 2L,
            title = "Read 20 pages",
            recurrence = RecurrenceType.DAILY
        )

        assertEquals("Daily", task.getFormattedRecurrenceText())
        assertTrue(task.isPendingOn(today))

        val completedToday = task.toggleCompletion(today)
        assertTrue(completedToday.isCompletedOn(today))
        assertFalse(completedToday.isPendingOn(today))

        assertFalse(completedToday.isCompletedOn(tomorrow))
        assertTrue(completedToday.isPendingOn(tomorrow))
    }

    @Test
    fun testWeekdaysTask_MondayToFridayOnly_AndNextDueDateSkipsWeekend() {
        val monday = LocalDate.of(2026, 11, 9)
        val friday = LocalDate.of(2026, 11, 13)
        val saturday = LocalDate.of(2026, 11, 14)
        val sunday = LocalDate.of(2026, 11, 15)
        val nextMonday = LocalDate.of(2026, 11, 16)

        val task = Task(
            id = 3L,
            title = "Standup meeting",
            recurrence = RecurrenceType.WEEKDAYS
        )

        assertEquals("Weekdays", task.getFormattedRecurrenceText())
        assertTrue(task.isDueOn(monday))
        assertTrue(task.isDueOn(friday))
        assertFalse(task.isDueOn(saturday))
        assertFalse(task.isDueOn(sunday))

        assertEquals(monday, task.getNextDueDate(monday))
        assertEquals(nextMonday, task.getNextDueDate(saturday))
        assertEquals(nextMonday, task.getNextDueDate(sunday))
    }

    @Test
    fun testWeeklyTask_CustomDaysOfWeek_DueAndFormatting() {
        val monday = LocalDate.of(2026, 11, 9)
        val tuesday = LocalDate.of(2026, 11, 10)
        val wednesday = LocalDate.of(2026, 11, 11)
        val friday = LocalDate.of(2026, 11, 13)
        val sunday = LocalDate.of(2026, 11, 15)

        val mwfTask = Task(
            id = 4L,
            title = "Gym",
            recurrence = RecurrenceType.WEEKLY,
            recurrenceDaysOfWeek = "MONDAY,WEDNESDAY,FRIDAY"
        )

        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            mwfTask.getSelectedDaysOfWeek()
        )
        assertEquals("Mon, Wed, Fri", mwfTask.getFormattedRecurrenceText())
        assertTrue(mwfTask.isDueOn(monday))
        assertFalse(mwfTask.isDueOn(tuesday))
        assertTrue(mwfTask.isDueOn(wednesday))
        assertTrue(mwfTask.isDueOn(friday))
        assertFalse(mwfTask.isDueOn(sunday))
        assertEquals(wednesday, mwfTask.getNextDueDate(tuesday))

        val everyDayWeekly = mwfTask.copy(
            recurrenceDaysOfWeek = DayOfWeek.entries.joinToString(",") { it.name }
        )
        assertEquals("Every day", everyDayWeekly.getFormattedRecurrenceText())

        val weekdaysWeekly = mwfTask.copy(
            recurrenceDaysOfWeek = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
        )
        assertEquals("Weekdays", weekdaysWeekly.getFormattedRecurrenceText())
    }

    @Test
    fun testMonthlyTask_Clamps31stToShortMonths_AndFormatsOrdinals() {
        val task31 = Task(
            id = 5L,
            title = "Month-end closing",
            recurrence = RecurrenceType.MONTHLY,
            recurrenceMonthDay = 31
        )

        assertTrue(task31.isDueOn(LocalDate.of(2026, 2, 28)))
        assertFalse(task31.isDueOn(LocalDate.of(2026, 2, 27)))
        assertTrue(task31.isDueOn(LocalDate.of(2028, 2, 29))) // Leap year
        assertFalse(task31.isDueOn(LocalDate.of(2028, 2, 28)))
        assertTrue(task31.isDueOn(LocalDate.of(2026, 4, 30)))
        assertFalse(task31.isDueOn(LocalDate.of(2026, 4, 29)))
        assertTrue(task31.isDueOn(LocalDate.of(2026, 5, 31)))
        assertFalse(task31.isDueOn(LocalDate.of(2026, 5, 30)))

        assertEquals("Monthly (1st)", task31.copy(recurrenceMonthDay = 1).getFormattedRecurrenceText())
        assertEquals("Monthly (2nd)", task31.copy(recurrenceMonthDay = 2).getFormattedRecurrenceText())
        assertEquals("Monthly (3rd)", task31.copy(recurrenceMonthDay = 3).getFormattedRecurrenceText())
        assertEquals("Monthly (11th)", task31.copy(recurrenceMonthDay = 11).getFormattedRecurrenceText())
        assertEquals("Monthly (21st)", task31.copy(recurrenceMonthDay = 21).getFormattedRecurrenceText())
        assertEquals("Monthly (31st)", task31.getFormattedRecurrenceText())
    }

    @Test
    fun testQuarterlyTask_DueEveryThreeMonths() {
        val created = LocalDate.of(2026, 1, 15)
        val task = Task(
            id = 6L,
            title = "Quarterly tax estimate",
            recurrence = RecurrenceType.QUARTERLY,
            recurrenceStartMonth = 1,
            recurrenceMonthDay = 15,
            createdAt = created.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        assertEquals("Quarterly (Jan 15th)", task.getFormattedRecurrenceText())
        assertTrue(task.isDueOn(LocalDate.of(2026, 1, 15)))
        assertFalse(task.isDueOn(LocalDate.of(2026, 2, 15)))
        assertFalse(task.isDueOn(LocalDate.of(2026, 3, 15)))
        assertTrue(task.isDueOn(LocalDate.of(2026, 4, 15)))
        assertTrue(task.isDueOn(LocalDate.of(2026, 7, 15)))
        assertTrue(task.isDueOn(LocalDate.of(2026, 10, 15)))
        assertTrue(task.isDueOn(LocalDate.of(2027, 1, 15)))
    }

    @Test
    fun testYearlyTask_DueOncePerYear() {
        val created = LocalDate.of(2026, 6, 20)
        val task = Task(
            id = 7L,
            title = "Annual car inspection",
            recurrence = RecurrenceType.YEARLY,
            recurrenceStartMonth = 6,
            recurrenceMonthDay = 20,
            createdAt = created.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        assertEquals("Yearly (Jun 20th)", task.getFormattedRecurrenceText())
        assertTrue(task.isDueOn(LocalDate.of(2026, 6, 20)))
        assertFalse(task.isDueOn(LocalDate.of(2026, 7, 20)))
        assertFalse(task.isDueOn(LocalDate.of(2025, 6, 20)))
        assertTrue(task.isDueOn(LocalDate.of(2027, 6, 20)))
    }

    @Test
    fun testPendingInNext7Days_AndTimeAndSnoozeFormatting() {
        val monday = LocalDate.of(2026, 11, 9)
        val thursdayTask = Task(
            id = 8L,
            title = "Thursday task",
            recurrence = RecurrenceType.WEEKLY,
            recurrenceDaysOfWeek = "THURSDAY",
            reminderHour = 9,
            reminderMinute = 5,
            snoozedUntil = System.currentTimeMillis() + 3_600_000L
        )

        assertFalse(thursdayTask.isPendingOn(monday))
        assertTrue(thursdayTask.isPendingInNext7Days(monday))

        // Monday is not in the next 7 days starting Tuesday (Nov 17 is 7 days ahead -> offset 0..6 covers Nov 10..Nov 16)
        val nextWeekCheck = monday.minusDays(8)
        assertFalse(thursdayTask.copy(recurrence = RecurrenceType.MONTHLY, recurrenceMonthDay = 30).isPendingInNext7Days(LocalDate.of(2026, 11, 10)))

        assertEquals("09:05", thursdayTask.getFormattedTimeText())
        assertNull(thursdayTask.copy(reminderHour = null).getFormattedTimeText())

        val snoozeText = thursdayTask.getFormattedSnoozeText()
        assertNotNull(snoozeText)
        assertTrue(snoozeText!!.startsWith("Snoozed until "))
        assertNull(thursdayTask.copy(snoozedUntil = System.currentTimeMillis() - 10_000L).getFormattedSnoozeText())

        val toggled = thursdayTask.toggleCompletion(monday)
        assertNull(toggled.snoozedUntil)
        assertEquals(nextWeekCheck.plusDays(8), monday)
    }
}
