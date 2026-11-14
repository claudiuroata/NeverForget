package com.neverforget.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neverforget.app.data.AppDatabase
import java.time.LocalDate

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val taskDao = database.taskDao()
            val allTasks = taskDao.getAllTasksSync()
            val today = LocalDate.now()

            // Overdue tasks from past dates
            val overdueTasks = allTasks.filter { it.isOverdue(today) }

            // Tasks pending for today
            val todayPendingTasks = allTasks.filter { it.isPendingOn(today) && !it.isOverdue(today) }

            // Tasks pending in next 7 days (excluding today & overdue)
            val upcomingTasks = allTasks.filter {
                it.isPendingInNext7Days(today) && !it.isPendingOn(today) && !it.isOverdue(today)
            }

            if (overdueTasks.isNotEmpty() || todayPendingTasks.isNotEmpty() || upcomingTasks.isNotEmpty()) {
                NotificationHelper.showPendingTasksNotification(
                    applicationContext,
                    todayTasks = todayPendingTasks,
                    upcomingTasks = upcomingTasks,
                    overdueTasks = overdueTasks
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
