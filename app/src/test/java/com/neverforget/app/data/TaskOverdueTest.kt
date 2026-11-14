package com.neverforget.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskOverdueTest {

    @Test
    fun testWeeklyTaskMarkedDoneNextDay_IsNotOverdueFollowingDays() {
        // Assume today is Monday, Oct 20, 2025
        val monday = LocalDate.of(2025, 10, 20)
        val sunday = LocalDate.of(2025, 10, 19)

        // Created on Sunday Oct 19, recurring on Sundays
        val task = Task(
            id = 1,
            title = "Weekly Review",
            recurrence = RecurrenceType.WEEKLY,
            recurrenceDaysOfWeek = "SUNDAY",
            createdAt = sunday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isCompleted = false,
            lastCompletedDate = null
        )

        // On Sunday: due today, not overdue yet
        assertTrue(task.isDueOn(sunday))
        assertFalse(task.isOverdue(sunday))

        // On Monday: overdue because Sunday was missed
        assertTrue(task.isOverdue(monday))

        // User marks it done on Monday
        val completedTask = task.toggleCompletion(monday)

        // On Monday: completed, so NOT overdue
        assertTrue(completedTask.isCompletedOn(monday))
        assertFalse(completedTask.isOverdue(monday))

        // On Tuesday (the following day): MUST NOT BE OVERDUE!
        val tuesday = LocalDate.of(2025, 10, 21)
        assertFalse("Task should not be overdue on Tuesday", completedTask.isOverdue(tuesday))

        // On Wednesday through Saturday: MUST NOT BE OVERDUE!
        for (dayOffset in 2L..5L) {
            val d = monday.plusDays(dayOffset)
            assertFalse("Task should not be overdue on $d", completedTask.isOverdue(d))
        }

        // On next Sunday (Oct 26): due for the new week!
        val nextSunday = LocalDate.of(2025, 10, 26)
        assertTrue(completedTask.isDueOn(nextSunday))
        assertTrue(completedTask.isPendingOn(nextSunday))
        assertFalse(completedTask.isCompletedOn(nextSunday))
    }

    @Test
    fun testDailyTaskMarkedDoneNextDay_IsNotOverdueFollowingDay() {
        val monday = LocalDate.of(2025, 10, 20)
        val tuesday = LocalDate.of(2025, 10, 21)
        val wednesday = LocalDate.of(2025, 10, 22)

        // Created Monday, missed Monday
        val task = Task(
            id = 2,
            title = "Daily Workout",
            recurrence = RecurrenceType.DAILY,
            createdAt = monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isCompleted = false,
            lastCompletedDate = null
        )

        // On Tuesday: overdue from Monday
        assertTrue(task.isOverdue(tuesday))

        // User marks it done on Tuesday
        val completedTask = task.toggleCompletion(tuesday)

        // On Tuesday: completed, not overdue
        assertTrue(completedTask.isCompletedOn(tuesday))
        assertFalse(completedTask.isOverdue(tuesday))

        // On Wednesday (the following day):
        // Yesterday (Tuesday) was completed, so it is NOT overdue on Wednesday!
        assertFalse("Daily task should not be overdue on Wednesday", completedTask.isOverdue(wednesday))
        // But Wednesday is a new day, so it is pending for today!
        assertTrue(completedTask.isPendingOn(wednesday))
    }

    @Test
    fun testMonthlyTaskMarkedDoneNextDay_IsNotOverdueRestOfMonth() {
        val firstOfMonth = LocalDate.of(2025, 10, 1)
        val secondOfMonth = LocalDate.of(2025, 10, 2)
        val thirdOfMonth = LocalDate.of(2025, 10, 3)

        val task = Task(
            id = 3,
            title = "Pay Rent",
            recurrence = RecurrenceType.MONTHLY,
            recurrenceMonthDay = 1,
            createdAt = firstOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isCompleted = false,
            lastCompletedDate = null
        )

        // Overdue on 2nd
        assertTrue(task.isOverdue(secondOfMonth))

        // Marked done on 2nd
        val completedTask = task.toggleCompletion(secondOfMonth)
        assertFalse(completedTask.isOverdue(secondOfMonth))

        // On 3rd: must NOT be overdue
        assertFalse(completedTask.isOverdue(thirdOfMonth))

        // Rest of the month: must NOT be overdue
        for (day in 4..31) {
            val d = LocalDate.of(2025, 10, day)
            assertFalse("Task should not be overdue on $d", completedTask.isOverdue(d))
        }
    }

    @Test
    fun testOneTimeTask_OverdueAfterCreationDayUntilCompleted() {
        val creationDay = LocalDate.of(2026, 11, 10)
        val nextDay = creationDay.plusDays(1)
        val twoDaysLater = creationDay.plusDays(2)

        val task = Task(
            id = 4,
            title = "Submit report",
            recurrence = RecurrenceType.NONE,
            createdAt = creationDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isCompleted = false
        )

        assertFalse(task.isOverdue(creationDay))
        assertTrue(task.isOverdue(nextDay))

        val completed = task.toggleCompletion(nextDay)
        assertFalse(completed.isOverdue(nextDay))
        assertFalse(completed.isOverdue(twoDaysLater))
    }

    @Test
    fun testWeekdaysTask_MissedFridayIsOverdueUntilCompletedMonday() {
        val friday = LocalDate.of(2026, 11, 13)
        val saturday = LocalDate.of(2026, 11, 14)
        val sunday = LocalDate.of(2026, 11, 15)
        val monday = LocalDate.of(2026, 11, 16)
        val tuesday = LocalDate.of(2026, 11, 17)

        val task = Task(
            id = 5,
            title = "Timesheet",
            recurrence = RecurrenceType.WEEKDAYS,
            createdAt = friday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        assertFalse(task.isOverdue(friday))
        assertTrue(task.isOverdue(saturday))
        assertTrue(task.isOverdue(sunday))
        assertTrue(task.isOverdue(monday))

        val completedMonday = task.toggleCompletion(monday)
        assertFalse(completedMonday.isOverdue(monday))
        assertFalse(completedMonday.isOverdue(tuesday))
    }

    @Test
    fun testQuarterlyAndYearlyTasks_OverdueAndClearedWhenMarkedDoneLate() {
        val jan15 = LocalDate.of(2026, 1, 15)
        val jan16 = LocalDate.of(2026, 1, 16)
        val feb10 = LocalDate.of(2026, 2, 10)
        val apr15 = LocalDate.of(2026, 4, 15)
        val apr16 = LocalDate.of(2026, 4, 16)

        val quarterly = Task(
            id = 6,
            title = "Quarterly audit",
            recurrence = RecurrenceType.QUARTERLY,
            recurrenceStartMonth = 1,
            recurrenceMonthDay = 15,
            createdAt = jan15.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        assertFalse(quarterly.isOverdue(jan15))
        assertTrue(quarterly.isOverdue(jan16))
        val doneJan16 = quarterly.toggleCompletion(jan16)
        assertFalse(doneJan16.isOverdue(jan16))
        assertFalse(doneJan16.isOverdue(feb10))
        // If Apr 15 is then missed, Apr 16 becomes overdue again
        assertTrue(doneJan16.isOverdue(apr16))
        assertFalse(doneJan16.isOverdue(apr15))

        val jun20 = LocalDate.of(2026, 6, 20)
        val jun21 = LocalDate.of(2026, 6, 21)
        val dec15 = LocalDate.of(2026, 12, 15)
        val yearly = Task(
            id = 7,
            title = "Yearly checkup",
            recurrence = RecurrenceType.YEARLY,
            recurrenceStartMonth = 6,
            recurrenceMonthDay = 20,
            createdAt = jun20.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        assertFalse(yearly.isOverdue(jun20))
        assertTrue(yearly.isOverdue(jun21))
        val doneJun21 = yearly.toggleCompletion(jun21)
        assertFalse(doneJun21.isOverdue(jun21))
        assertFalse(doneJun21.isOverdue(dec15))
    }
}
