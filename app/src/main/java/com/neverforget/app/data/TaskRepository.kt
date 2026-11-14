package com.neverforget.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun toggleTaskCompletion(task: Task, date: LocalDate = LocalDate.now()) {
        val updated = task.toggleCompletion(date)
        taskDao.updateTask(updated)
    }

    suspend fun getPendingTasksForToday(): List<Task> {
        val today = LocalDate.now()
        val all = taskDao.getAllTasksSync()
        return all.filter { it.isPendingOn(today) }
    }

    suspend fun clearCompletedOneTimeTasks() {
        taskDao.clearCompletedOneTimeTasks()
    }
}
