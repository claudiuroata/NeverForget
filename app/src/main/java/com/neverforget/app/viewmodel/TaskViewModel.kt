package com.neverforget.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neverforget.app.TodoApplication
import com.neverforget.app.data.BackupManager
import com.neverforget.app.data.MealContext
import com.neverforget.app.data.Pill
import com.neverforget.app.data.PillRepository
import com.neverforget.app.data.RecurrenceType
import com.neverforget.app.data.Task
import com.neverforget.app.data.TaskRepository
import com.neverforget.app.reminder.NotificationHelper
import com.neverforget.app.reminder.ReminderScheduler
import com.neverforget.app.ui.theme.ThemeMode
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppWorkspace(val label: String) {
    TASKS("Tasks"),
    PILLS("Pills")
}

enum class PillFilter(val label: String) {
    TODAY_DOSES("Today's Doses"),
    ACTIVE_MEDS("Active Meds"),
    FINISHED("Finished")
}

data class ScheduledDoseItem(
    val pill: Pill,
    val timeSlot: String,
    val isTaken: Boolean
)

data class PillStats(
    val totalDosesToday: Int = 0,
    val takenDosesToday: Int = 0,
    val pendingDosesToday: Int = 0,
    val activeMedsCount: Int = 0,
    val finishedMedsCount: Int = 0,
    val progress: Float = 0f
)

enum class TaskFilter(val label: String) {
    TODAY_PENDING("Pending Today"),
    OVERDUE("Overdue"),
    NEXT_7_DAYS("Next 7 Days"),
    ALL("All Tasks"),
    TODAY_COMPLETED("Done"),
    RECURRING("Recurring")
}

data class TaskStats(
    val totalDueToday: Int = 0,
    val completedToday: Int = 0,
    val pendingToday: Int = 0,
    val overdueCount: Int = 0,
    val pendingNext7Days: Int = 0,
    val totalAllTasks: Int = 0,
    val totalRecurringTasks: Int = 0,
    val progress: Float = 0f
)

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository,
    private val pillRepository: PillRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)

    private val _selectedWorkspace = MutableStateFlow(AppWorkspace.TASKS)
    val selectedWorkspace: StateFlow<AppWorkspace> = _selectedWorkspace.asStateFlow()

    private val _selectedPillFilter = MutableStateFlow(PillFilter.TODAY_DOSES)
    val selectedPillFilter: StateFlow<PillFilter> = _selectedPillFilter.asStateFlow()

    private val _selectedNotificationPill = MutableStateFlow<Pair<Pill, String>?>(null)
    val selectedNotificationPill: StateFlow<Pair<Pill, String>?> = _selectedNotificationPill.asStateFlow()

    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedNotificationTask = MutableStateFlow<Task?>(null)
    val selectedNotificationTask: StateFlow<Task?> = _selectedNotificationTask.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.TODAY_PENDING)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    // Reminder state
    private val _reminderEnabled = MutableStateFlow(
        ReminderScheduler.isReminderEnabled(application)
    )
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val initialTime = ReminderScheduler.getReminderTime(application)
    private val _reminderHour = MutableStateFlow(initialTime.first)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(initialTime.second)
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPills: StateFlow<List<Pill>> = pillRepository.allPills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDoseItems: StateFlow<List<ScheduledDoseItem>> = combine(
        allPills,
        today
    ) { pills, currentDate ->
        pills.filter { it.isActiveOn(currentDate) }
            .flatMap { pill ->
                pill.getScheduledTimes().map { timeSlot ->
                    ScheduledDoseItem(
                        pill = pill,
                        timeSlot = timeSlot,
                        isTaken = pill.isDoseTaken(currentDate, timeSlot)
                    )
                }
            }
            .sortedWith(
                compareBy(
                    { it.isTaken },
                    { it.timeSlot },
                    { it.pill.name.lowercase() }
                )
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pillStats: StateFlow<PillStats> = combine(
        allPills,
        today
    ) { pills, currentDate ->
        val activeMeds = pills.filter { !it.isCourseCompleted(currentDate) }
        val finishedMeds = pills.filter { it.isCourseCompleted(currentDate) }
        val todayDoses = pills.filter { it.isActiveOn(currentDate) }
            .flatMap { pill ->
                pill.getScheduledTimes().map { timeSlot ->
                    pill.isDoseTaken(currentDate, timeSlot)
                }
            }
        val totalDoses = todayDoses.size
        val takenDoses = todayDoses.count { it }
        val pendingDoses = totalDoses - takenDoses
        val progress = if (totalDoses > 0) takenDoses.toFloat() / totalDoses.toFloat() else 0f
        PillStats(
            totalDosesToday = totalDoses,
            takenDosesToday = takenDoses,
            pendingDosesToday = pendingDoses,
            activeMedsCount = activeMeds.size,
            finishedMedsCount = finishedMeds.size,
            progress = progress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PillStats())

    // Combined filtered tasks based on filter and today's date
    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks,
        selectedFilter,
        today
    ) { tasks, filter, currentDate ->
        val matchingTasks = when (filter) {
            TaskFilter.TODAY_PENDING -> {
                tasks.filter { it.isPendingOn(currentDate) }
            }
            TaskFilter.OVERDUE -> {
                tasks.filter { it.isOverdue(currentDate) }
            }
            TaskFilter.NEXT_7_DAYS -> {
                tasks.filter { it.isPendingInNext7Days(currentDate) }
            }
            TaskFilter.ALL -> tasks
            TaskFilter.TODAY_COMPLETED -> {
                tasks.filter { it.isCompletedOn(currentDate) }
            }
            TaskFilter.RECURRING -> {
                tasks.filter { it.recurrence.isRecurring }
            }
        }
        sortTasksByDueDateTime(matchingTasks, currentDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun sortTasksByDueDateTime(tasks: List<Task>, currentDate: LocalDate): List<Task> {
        return tasks.sortedWith(
            compareBy(
                { it.getNextDueDate(currentDate) },
                { it.reminderHour ?: 24 },
                { it.reminderMinute ?: 60 },
                { it.title.lowercase() }
            )
        )
    }

    // Progress statistics for today
    val stats: StateFlow<TaskStats> = combine(
        allTasks,
        today
    ) { tasks, currentDate ->
        val dueToday = tasks.filter { it.isDueOn(currentDate) }
        val completedTodayDue = dueToday.count { it.isCompletedOn(currentDate) }
        val completedTabCount = tasks.count { it.isCompletedOn(currentDate) }
        val pending = dueToday.count { it.isPendingOn(currentDate) }
        val overdue = tasks.count { it.isOverdue(currentDate) }
        val pending7Days = tasks.count { it.isPendingInNext7Days(currentDate) }
        val totalAll = tasks.size
        val totalRecurring = tasks.count { it.recurrence.isRecurring }
        val total = dueToday.size
        val progress = if (total > 0) completedTodayDue.toFloat() / total.toFloat() else 0f
        TaskStats(
            totalDueToday = total,
            completedToday = completedTabCount,
            pendingToday = pending,
            overdueCount = overdue,
            pendingNext7Days = pending7Days,
            totalAllTasks = totalAll,
            totalRecurringTasks = totalRecurring,
            progress = progress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskStats())

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun toggleThemeMode() {
        setThemeMode(_themeMode.value.next())
    }

    fun openTaskDetail(taskId: Long) {
        if (taskId == -1L) return
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                _selectedNotificationTask.value = task
            }
        }
    }

    fun dismissTaskDetail() {
        _selectedNotificationTask.value = null
    }

    fun snoozeTask(task: Task, hours: Int) {
        viewModelScope.launch {
            val snoozeTimeMillis = System.currentTimeMillis() + (hours.toLong() * 3600 * 1000)
            val updatedTask = task.copy(snoozedUntil = snoozeTimeMillis)
            repository.updateTask(updatedTask)
            ReminderScheduler.scheduleExactSnooze(getApplication(), hours, task.id)
            _selectedNotificationTask.value = null
        }
    }

    fun cancelSnooze(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(snoozedUntil = null)
            repository.updateTask(updatedTask)
        }
    }

    fun setFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun refreshDate() {
        _today.value = LocalDate.now()
    }

    fun addTask(
        title: String,
        description: String = "",
        recurrence: RecurrenceType = RecurrenceType.NONE,
        recurrenceDaysOfWeek: String = "",
        recurrenceMonthDay: Int? = null,
        recurrenceStartMonth: Int? = null,
        reminderHour: Int? = null,
        reminderMinute: Int? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val newTask = Task(
                title = title.trim(),
                description = description.trim(),
                recurrence = recurrence,
                recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                recurrenceMonthDay = recurrenceMonthDay,
                recurrenceStartMonth = recurrenceStartMonth,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                createdAt = System.currentTimeMillis()
            )
            val newId = repository.insertTask(newTask)
            if (reminderHour != null) {
                val insertedTask = newTask.copy(id = newId)
                ReminderScheduler.scheduleTaskReminder(getApplication(), insertedTask)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            if (task.reminderHour != null) {
                ReminderScheduler.scheduleTaskReminder(getApplication(), task)
            } else {
                ReminderScheduler.cancelTaskReminder(getApplication(), task.id)
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task, _today.value)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderScheduler.cancelTaskReminder(getApplication(), task.id)
        }
    }

    fun exportBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = allTasks.value
            val pills = allPills.value
            val success = BackupManager.exportTasksToJson(getApplication(), tasks, uri, pills)
            launch(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun importBackup(uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = BackupManager.importBackupFromJson(getApplication(), uri)
            if (imported != null) {
                val currentDate = _today.value
                imported.tasks.forEach { task ->
                    val newId = repository.insertTask(task)
                    if (task.reminderHour != null) {
                        ReminderScheduler.scheduleTaskReminder(getApplication(), task.copy(id = newId))
                    }
                }
                imported.pills.forEach { pill ->
                    val newId = pillRepository.insertPill(pill)
                    val insertedPill = pill.copy(id = newId)
                    if (!insertedPill.isCourseCompleted(currentDate)) {
                        ReminderScheduler.schedulePillReminders(getApplication(), insertedPill)
                    }
                }
                val totalRestored = imported.tasks.size + imported.pills.size
                launch(Dispatchers.Main) {
                    onResult(totalRestored)
                }
            } else {
                launch(Dispatchers.Main) {
                    onResult(-1)
                }
            }
        }
    }

    fun clearCompletedOneTime() {
        viewModelScope.launch {
            repository.clearCompletedOneTimeTasks()
        }
    }

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        _reminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute
        ReminderScheduler.saveReminderSettings(getApplication(), enabled, hour, minute)
    }

    fun testReminderNow() {
        ReminderScheduler.triggerImmediateCheck(getApplication())
    }

    // --- Pills Workspace Actions ---

    fun setWorkspace(workspace: AppWorkspace) {
        _selectedWorkspace.value = workspace
    }

    fun setPillFilter(filter: PillFilter) {
        _selectedPillFilter.value = filter
    }

    fun openPillDetail(pillId: Long, timeSlot: String) {
        if (pillId == -1L) return
        _selectedWorkspace.value = AppWorkspace.PILLS
        viewModelScope.launch {
            val pill = pillRepository.getPillById(pillId)
            if (pill != null) {
                val resolvedSlot = timeSlot.ifBlank {
                    pill.getScheduledTimes().firstOrNull() ?: "08:00"
                }
                _selectedNotificationPill.value = pill to resolvedSlot
            }
        }
    }

    fun dismissPillDetail() {
        _selectedNotificationPill.value = null
    }

    fun addPill(
        name: String,
        dosage: String,
        notes: String,
        mealContext: MealContext,
        doseTimes: List<String>,
        startDateEpochDay: Long = _today.value.toEpochDay(),
        durationDays: Int? = null
    ) {
        if (name.isBlank()) return
        val normalizedTimes = doseTimes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .ifEmpty { listOf("08:00") }

        viewModelScope.launch {
            val newPill = Pill(
                name = name.trim(),
                dosage = dosage.trim().ifBlank { "1 pill" },
                notes = notes.trim(),
                mealContext = mealContext,
                doseTimes = normalizedTimes.joinToString(","),
                startDateEpochDay = startDateEpochDay,
                durationDays = durationDays?.takeIf { it > 0 },
                createdAt = System.currentTimeMillis()
            )
            val newId = pillRepository.insertPill(newPill)
            val inserted = newPill.copy(id = newId)
            ReminderScheduler.schedulePillReminders(getApplication(), inserted)
        }
    }

    fun updatePill(pill: Pill) {
        viewModelScope.launch {
            pillRepository.updatePill(pill)
            if (pill.isCourseCompleted(_today.value)) {
                ReminderScheduler.cancelPillReminders(getApplication(), pill.id)
            } else {
                ReminderScheduler.schedulePillReminders(getApplication(), pill)
            }
        }
    }

    fun deletePill(pill: Pill) {
        viewModelScope.launch {
            pillRepository.deletePill(pill)
            ReminderScheduler.cancelPillReminders(getApplication(), pill.id)
        }
    }

    fun togglePillDose(pill: Pill, timeSlot: String) {
        viewModelScope.launch {
            val currentDate = _today.value
            val updated = pill.toggleDoseTaken(currentDate, timeSlot)
            pillRepository.updatePill(updated)
            if (updated.isCourseCompleted(currentDate)) {
                ReminderScheduler.cancelPillReminders(getApplication(), updated.id)
            } else {
                ReminderScheduler.schedulePillReminders(getApplication(), updated)
            }
        }
    }

    fun snoozePillDose(pill: Pill, timeSlot: String, minutes: Int) {
        viewModelScope.launch {
            val snoozeUntil = System.currentTimeMillis() + (minutes.toLong() * 60_000L)
            val slotKey = Pill.buildSlotKey(_today.value, timeSlot)
            val updated = pill.copy(
                snoozedSlotKey = slotKey,
                snoozedUntil = snoozeUntil
            )
            pillRepository.updatePill(updated)
            ReminderScheduler.schedulePillSnooze(getApplication(), pill.id, timeSlot, minutes)
            _selectedNotificationPill.value = null
        }
    }

    fun testPillReminderNow(pill: Pill? = null) {
        val targetPill = pill ?: todayDoseItems.value.firstOrNull { !it.isTaken }?.pill
            ?: allPills.value.firstOrNull()

        if (targetPill == null) {
            Toast.makeText(
                getApplication(),
                "Add a medication first to test its pill alert!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val targetSlot = if (pill == null) {
            todayDoseItems.value.firstOrNull { !it.isTaken && it.pill.id == targetPill.id }?.timeSlot
                ?: targetPill.getScheduledTimes().firstOrNull()
                ?: "08:00"
        } else {
            targetPill.getScheduledTimes().firstOrNull() ?: "08:00"
        }

        NotificationHelper.showPillDoseNotification(getApplication(), targetPill, targetSlot)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = application as TodoApplication
            return TaskViewModel(application, app.repository, app.pillRepository) as T
        }
    }
}
