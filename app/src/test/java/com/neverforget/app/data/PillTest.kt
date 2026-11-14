package com.neverforget.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PillTest {

    @Test
    fun testMultipleDosesPerDay_TracksSlotsIndependentlyAndResetsNextDay() {
        val today = LocalDate.of(2026, 11, 14)
        val tomorrow = today.plusDays(1)

        val pill = Pill(
            id = 1L,
            name = "Augmentin",
            dosage = "1 tablet",
            mealContext = MealContext.WITH_MEAL,
            doseTimes = "20:00, 08:00",
            startDateEpochDay = today.toEpochDay(),
            durationDays = null
        )

        assertEquals(listOf("08:00", "20:00"), pill.getScheduledTimes())
        assertFalse(pill.isDoseTaken(today, "08:00"))
        assertFalse(pill.isDoseTaken(today, "20:00"))

        // Mark only 08:00 taken today
        val afterMorning = pill.toggleDoseTaken(today, "08:00")
        assertTrue(afterMorning.isDoseTaken(today, "08:00"))
        assertFalse(afterMorning.isDoseTaken(today, "20:00"))

        // Tomorrow both doses are pending again
        assertFalse(afterMorning.isDoseTaken(tomorrow, "08:00"))
        assertFalse(afterMorning.isDoseTaken(tomorrow, "20:00"))

        // Unchecking 08:00 today returns it to untaken
        val unchecked = afterMorning.toggleDoseTaken(today, "08:00")
        assertFalse(unchecked.isDoseTaken(today, "08:00"))

        // Idempotent markDoseTaken keeps it taken once
        val markedTwice = unchecked.markDoseTaken(today, "08:00").markDoseTaken(today, "08:00")
        assertTrue(markedTwice.isDoseTaken(today, "08:00"))
        assertFalse(markedTwice.isDoseTaken(today, "20:00"))
    }

    @Test
    fun testCourseDuration_ActiveDaysAndCompletionBoundary() {
        val startDate = LocalDate.of(2026, 11, 14)
        val pill = Pill(
            id = 2L,
            name = "Antibiotic",
            dosage = "500mg",
            mealContext = MealContext.AFTER_MEAL,
            doseTimes = "08:00,20:00",
            startDateEpochDay = startDate.toEpochDay(),
            durationDays = 7
        )

        // Before start date: not active
        assertFalse(pill.isActiveOn(startDate.minusDays(1)))

        // Days 0..6 from startDate: active
        for (offset in 0L..6L) {
            assertTrue("Expected active on day offset $offset", pill.isActiveOn(startDate.plusDays(offset)))
        }
        assertEquals("Day 1 of 7", pill.getCourseProgressText(startDate))
        assertEquals("Day 7 of 7", pill.getCourseProgressText(startDate.plusDays(6)))
        assertFalse(pill.isCourseCompleted(startDate.plusDays(6)))

        // Day 7 (8th day): course finished, not active
        val dayAfterCourse = startDate.plusDays(7)
        assertFalse(pill.isActiveOn(dayAfterCourse))
        assertTrue(pill.isCourseCompleted(dayAfterCourse))
        assertEquals("Finished (7 days)", pill.getCourseProgressText(dayAfterCourse))

        // Finishing all doses on the final day (offset 6) marks course completed immediately
        val lastDay = startDate.plusDays(6)
        val finishedOnLastDay = pill.markDoseTaken(lastDay, "08:00").markDoseTaken(lastDay, "20:00")
        assertTrue(finishedOnLastDay.isCourseCompleted(lastDay))
    }

    @Test
    fun testOngoingMedication_AlwaysActiveAndNeverCourseCompleted() {
        val startDate = LocalDate.of(2026, 11, 14)
        val ongoingPill = Pill(
            id = 3L,
            name = "Vitamin D3",
            dosage = "2000 IU",
            mealContext = MealContext.WITH_MEAL,
            doseTimes = "09:00",
            startDateEpochDay = startDate.toEpochDay(),
            durationDays = null
        )

        assertFalse(ongoingPill.isActiveOn(startDate.minusDays(1)))
        assertTrue(ongoingPill.isActiveOn(startDate))
        assertTrue(ongoingPill.isActiveOn(startDate.plusDays(1000)))
        assertFalse(ongoingPill.isCourseCompleted(startDate.plusDays(1000)))
        assertEquals("Ongoing", ongoingPill.getCourseProgressText(startDate.plusDays(100)))
    }

    @Test
    fun testPillSnooze_SlotSpecificFormattingAndClearedOnDoseTaken() {
        val today = LocalDate.of(2026, 11, 14)
        val futureMillis = System.currentTimeMillis() + 900_000L
        val pastMillis = System.currentTimeMillis() - 10_000L

        val snoozedPill = Pill(
            id = 4L,
            name = "Omega-3",
            doseTimes = "08:00,20:00",
            startDateEpochDay = today.toEpochDay(),
            snoozedSlotKey = Pill.buildSlotKey(today, "08:00"),
            snoozedUntil = futureMillis
        )

        val snooze0800 = snoozedPill.getFormattedSnoozeText(today, "08:00")
        assertNotNull(snooze0800)
        assertTrue(snooze0800!!.startsWith("Snoozed until "))
        assertNull(snoozedPill.getFormattedSnoozeText(today, "20:00"))
        assertNull(snoozedPill.copy(snoozedUntil = pastMillis).getFormattedSnoozeText(today, "08:00"))

        val afterMarkTaken = snoozedPill.markDoseTaken(today, "08:00")
        assertNull(afterMarkTaken.snoozedSlotKey)
        assertNull(afterMarkTaken.snoozedUntil)
        assertNull(afterMarkTaken.getFormattedSnoozeText(today, "08:00"))

        val afterToggleTaken = snoozedPill.toggleDoseTaken(today, "08:00")
        assertNull(afterToggleTaken.snoozedSlotKey)
        assertNull(afterToggleTaken.snoozedUntil)
    }

    @Test
    fun testTakenHistoryPruning_CapsAt120Entries() {
        val startDate = LocalDate.of(2026, 1, 1)
        var pill = Pill(
            id = 5L,
            name = "Magnesium",
            doseTimes = "08:00",
            startDateEpochDay = startDate.toEpochDay()
        )

        for (day in 0 until 130) {
            pill = pill.markDoseTaken(startDate.plusDays(day.toLong()), "08:00")
        }

        // Oldest 10 days (0..9) should have been pruned
        for (day in 0 until 10) {
            assertFalse("Expected day $day to be pruned", pill.isDoseTaken(startDate.plusDays(day.toLong()), "08:00"))
        }

        // Most recent 120 days (10..129) should remain
        for (day in 10 until 130) {
            assertTrue("Expected day $day to be kept", pill.isDoseTaken(startDate.plusDays(day.toLong()), "08:00"))
        }
    }
}
