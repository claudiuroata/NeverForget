package com.neverforget.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

data class BackupData(
    val tasks: List<Task>,
    val pills: List<Pill> = emptyList()
)

object BackupManager {

    private const val BACKUP_VERSION = 2

    fun serializeBackupToJson(
        tasks: List<Task>,
        pills: List<Pill> = emptyList(),
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("app", "NeverForget")
        root.put("exportedAt", exportedAt)

        val tasksArray = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("recurrence", task.recurrence.name)
                put("recurrenceDaysOfWeek", task.recurrenceDaysOfWeek)
                task.recurrenceMonthDay?.let { put("recurrenceMonthDay", it) }
                task.recurrenceStartMonth?.let { put("recurrenceStartMonth", it) }
                put("createdAt", task.createdAt)
                put("isCompleted", task.isCompleted)
                task.lastCompletedDate?.let { put("lastCompletedDate", it) }
                task.reminderHour?.let { put("reminderHour", it) }
                task.reminderMinute?.let { put("reminderMinute", it) }
                task.snoozedUntil?.let { put("snoozedUntil", it) }
            }
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        val pillsArray = JSONArray()
        pills.forEach { pill ->
            val obj = JSONObject().apply {
                put("id", pill.id)
                put("name", pill.name)
                put("dosage", pill.dosage)
                put("notes", pill.notes)
                put("mealContext", pill.mealContext.name)
                put("doseTimes", pill.doseTimes)
                put("startDateEpochDay", pill.startDateEpochDay)
                pill.durationDays?.let { put("durationDays", it) }
                put("takenHistory", pill.takenHistory)
                pill.snoozedSlotKey?.let { put("snoozedSlotKey", it) }
                pill.snoozedUntil?.let { put("snoozedUntil", it) }
                put("createdAt", pill.createdAt)
            }
            pillsArray.put(obj)
        }
        root.put("pills", pillsArray)

        return root.toString(2)
    }

    fun deserializeBackupFromJson(jsonString: String): BackupData? {
        return try {
            val root = JSONObject(jsonString)
            val importedTasks = mutableListOf<Task>()
            val tasksArray = root.optJSONArray("tasks")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)
                    val recurrenceStr = obj.optString("recurrence", RecurrenceType.NONE.name)
                    val recurrenceType = try {
                        RecurrenceType.valueOf(recurrenceStr)
                    } catch (e: Exception) {
                        RecurrenceType.NONE
                    }

                    val task = Task(
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        recurrence = recurrenceType,
                        recurrenceDaysOfWeek = obj.optString("recurrenceDaysOfWeek", ""),
                        recurrenceMonthDay = if (obj.has("recurrenceMonthDay") && !obj.isNull("recurrenceMonthDay")) obj.getInt("recurrenceMonthDay") else null,
                        recurrenceStartMonth = if (obj.has("recurrenceStartMonth") && !obj.isNull("recurrenceStartMonth")) obj.getInt("recurrenceStartMonth") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        lastCompletedDate = if (obj.has("lastCompletedDate") && !obj.isNull("lastCompletedDate")) obj.getString("lastCompletedDate") else null,
                        reminderHour = if (obj.has("reminderHour") && !obj.isNull("reminderHour")) obj.getInt("reminderHour") else null,
                        reminderMinute = if (obj.has("reminderMinute") && !obj.isNull("reminderMinute")) obj.getInt("reminderMinute") else null,
                        snoozedUntil = if (obj.has("snoozedUntil") && !obj.isNull("snoozedUntil")) obj.getLong("snoozedUntil") else null
                    )
                    if (task.title.isNotBlank()) {
                        importedTasks.add(task)
                    }
                }
            }

            val importedPills = mutableListOf<Pill>()
            val pillsArray = root.optJSONArray("pills")
            if (pillsArray != null) {
                val defaultStartDay = LocalDate.now().toEpochDay()
                for (i in 0 until pillsArray.length()) {
                    val obj = pillsArray.getJSONObject(i)
                    val mealStr = obj.optString("mealContext", MealContext.ANYTIME.name)
                    val mealContext = try {
                        MealContext.valueOf(mealStr)
                    } catch (e: Exception) {
                        MealContext.ANYTIME
                    }

                    val pill = Pill(
                        name = obj.optString("name", ""),
                        dosage = obj.optString("dosage", "1 pill"),
                        notes = obj.optString("notes", ""),
                        mealContext = mealContext,
                        doseTimes = obj.optString("doseTimes", "08:00"),
                        startDateEpochDay = obj.optLong("startDateEpochDay", defaultStartDay),
                        durationDays = if (obj.has("durationDays") && !obj.isNull("durationDays")) obj.getInt("durationDays") else null,
                        takenHistory = obj.optString("takenHistory", ""),
                        snoozedSlotKey = if (obj.has("snoozedSlotKey") && !obj.isNull("snoozedSlotKey")) obj.getString("snoozedSlotKey") else null,
                        snoozedUntil = if (obj.has("snoozedUntil") && !obj.isNull("snoozedUntil")) obj.getLong("snoozedUntil") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    if (pill.name.isNotBlank()) {
                        importedPills.add(pill)
                    }
                }
            }

            BackupData(tasks = importedTasks, pills = importedPills)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportTasksToJson(
        context: Context,
        tasks: List<Task>,
        uri: Uri,
        pills: List<Pill> = emptyList()
    ): Boolean {
        return try {
            val serialized = serializeBackupToJson(tasks = tasks, pills = pills)
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(serialized.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importBackupFromJson(context: Context, uri: Uri): BackupData? {
        return try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            deserializeBackupFromJson(stringBuilder.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importTasksFromJson(context: Context, uri: Uri): List<Task>? {
        return importBackupFromJson(context, uri)?.tasks
    }
}
