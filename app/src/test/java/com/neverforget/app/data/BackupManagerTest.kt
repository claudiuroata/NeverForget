package com.neverforget.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupManagerTest {

    @Test
    fun testSerializeAndDeserializeBackup_RoundTripsTasksAndPills() {
        val today = LocalDate.of(2026, 11, 14)
        val tasks = listOf(
            Task(
                id = 10L,
                title = "Pay rent",
                description = "Bank transfer",
                recurrence = RecurrenceType.MONTHLY,
                recurrenceMonthDay = 1,
                createdAt = 1_700_000_000_000L,
                isCompleted = false,
                lastCompletedDate = "2026-10-01",
                reminderHour = 9,
                reminderMinute = 30,
                snoozedUntil = 1_700_003_600_000L
            ),
            Task(
                id = 11L,
                title = "Team sync",
                recurrence = RecurrenceType.WEEKLY,
                recurrenceDaysOfWeek = "MONDAY,THURSDAY",
                createdAt = 1_700_000_100_000L,
                isCompleted = true,
                lastCompletedDate = "2026-11-12"
            )
        )

        val pills = listOf(
            Pill(
                id = 5L,
                name = "Amoxicillin",
                dosage = "500mg capsule",
                notes = "Take with water",
                mealContext = MealContext.AFTER_MEAL,
                doseTimes = "08:00,20:00",
                startDateEpochDay = today.toEpochDay(),
                durationDays = 10,
                takenHistory = "2026-11-14@08:00",
                snoozedSlotKey = "2026-11-14@20:00",
                snoozedUntil = 1_700_007_200_000L,
                createdAt = 1_700_000_200_000L
            )
        )

        val json = BackupManager.serializeBackupToJson(
            tasks = tasks,
            pills = pills,
            exportedAt = 1_700_000_500_000L
        )
        val restored = BackupManager.deserializeBackupFromJson(json)

        assertNotNull(restored)
        assertEquals(2, restored!!.tasks.size)
        assertEquals(1, restored.pills.size)

        val t0 = restored.tasks[0]
        assertEquals("Pay rent", t0.title)
        assertEquals("Bank transfer", t0.description)
        assertEquals(RecurrenceType.MONTHLY, t0.recurrence)
        assertEquals(1, t0.recurrenceMonthDay)
        assertNull(t0.recurrenceStartMonth)
        assertEquals("2026-10-01", t0.lastCompletedDate)
        assertEquals(9, t0.reminderHour)
        assertEquals(30, t0.reminderMinute)
        assertEquals(1_700_003_600_000L, t0.snoozedUntil)

        val t1 = restored.tasks[1]
        assertEquals("Team sync", t1.title)
        assertEquals(RecurrenceType.WEEKLY, t1.recurrence)
        assertEquals("MONDAY,THURSDAY", t1.recurrenceDaysOfWeek)
        assertTrue(t1.isCompleted)

        val p0 = restored.pills[0]
        assertEquals("Amoxicillin", p0.name)
        assertEquals("500mg capsule", p0.dosage)
        assertEquals("Take with water", p0.notes)
        assertEquals(MealContext.AFTER_MEAL, p0.mealContext)
        assertEquals("08:00,20:00", p0.doseTimes)
        assertEquals(today.toEpochDay(), p0.startDateEpochDay)
        assertEquals(10, p0.durationDays)
        assertEquals("2026-11-14@08:00", p0.takenHistory)
        assertEquals("2026-11-14@20:00", p0.snoozedSlotKey)
        assertEquals(1_700_007_200_000L, p0.snoozedUntil)
    }

    @Test
    fun testDeserializeLegacyV1TasksOnlyJson_BackwardCompatible() {
        val v1Json = """
            {
              "version": 1,
              "app": "NuUita",
              "exportedAt": 1700000000000,
              "tasks": [
                {
                  "id": 1,
                  "title": "Drink water",
                  "description": "Stay hydrated",
                  "recurrence": "DAILY",
                  "recurrenceDaysOfWeek": "",
                  "createdAt": 1700000000000,
                  "isCompleted": false
                }
              ]
            }
        """.trimIndent()

        val restored = BackupManager.deserializeBackupFromJson(v1Json)
        assertNotNull(restored)
        assertEquals(1, restored!!.tasks.size)
        assertEquals("Drink water", restored.tasks[0].title)
        assertEquals(RecurrenceType.DAILY, restored.tasks[0].recurrence)
        assertFalse(restored.tasks[0].isCompleted)
        assertTrue(restored.pills.isEmpty())
    }

    @Test
    fun testDeserializeInvalidJsonAndBlankTitles_HandledGracefully() {
        assertNull(BackupManager.deserializeBackupFromJson("{not-valid-json"))
        assertNull(BackupManager.deserializeBackupFromJson(""))

        val mixedJson = """
            {
              "version": 2,
              "tasks": [
                { "title": "   ", "recurrence": "DAILY" },
                { "title": "Valid task", "recurrence": "FUTURE_UNKNOWN" }
              ],
              "pills": [
                { "name": "" },
                { "name": "Vitamin C", "mealContext": "UNKNOWN_MEAL" }
              ]
            }
        """.trimIndent()

        val restored = BackupManager.deserializeBackupFromJson(mixedJson)
        assertNotNull(restored)
        assertEquals(1, restored!!.tasks.size)
        assertEquals("Valid task", restored.tasks[0].title)
        assertEquals(RecurrenceType.NONE, restored.tasks[0].recurrence)

        assertEquals(1, restored.pills.size)
        assertEquals("Vitamin C", restored.pills[0].name)
        assertEquals(MealContext.ANYTIME, restored.pills[0].mealContext)
    }
}
