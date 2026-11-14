package com.neverforget.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neverforget.app.data.Pill
import com.neverforget.app.data.Task
import com.neverforget.app.ui.components.*
import com.neverforget.app.ui.theme.ThemeMode
import com.neverforget.app.viewmodel.AppWorkspace
import com.neverforget.app.viewmodel.PillFilter
import com.neverforget.app.viewmodel.TaskFilter
import com.neverforget.app.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val today by viewModel.today.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val workspace by viewModel.selectedWorkspace.collectAsState()
    val pillFilter by viewModel.selectedPillFilter.collectAsState()
    val allPills by viewModel.allPills.collectAsState()
    val todayDoses by viewModel.todayDoseItems.collectAsState()
    val pillStats by viewModel.pillStats.collectAsState()
    val selectedNotificationPill by viewModel.selectedNotificationPill.collectAsState()

    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val selectedNotificationTask by viewModel.selectedNotificationTask.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showAddPillDialog by remember { mutableStateOf(false) }
    var pillToEdit by remember { mutableStateOf<Pill?>(null) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val filters = TaskFilter.entries.toTypedArray()
    val pillFilters = PillFilter.entries.toTypedArray()
    val coroutineScope = rememberCoroutineScope()
    val filterListState = rememberLazyListState()
    val pillFilterListState = rememberLazyListState()

    val pagerState = rememberPagerState(
        initialPage = filters.indexOf(filter).coerceAtLeast(0),
        pageCount = { filters.size }
    )
    val pillPagerState = rememberPagerState(
        initialPage = pillFilters.indexOf(pillFilter).coerceAtLeast(0),
        pageCount = { pillFilters.size }
    )

    // Sync ViewModel filter and auto-scroll filter bar when user swipes pager
    LaunchedEffect(pagerState.currentPage) {
        val newFilter = filters[pagerState.currentPage]
        if (newFilter != filter) {
            viewModel.setFilter(newFilter)
        }
        filterListState.animateScrollToItem(pagerState.currentPage)
    }

    // Sync pager state when user taps a filter chip
    LaunchedEffect(filter) {
        val targetPage = filters.indexOf(filter)
        if (targetPage != -1 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync Pill filter and pill pager state
    LaunchedEffect(pillPagerState.currentPage) {
        val newFilter = pillFilters[pillPagerState.currentPage]
        if (newFilter != pillFilter) {
            viewModel.setPillFilter(newFilter)
        }
        pillFilterListState.animateScrollToItem(pillPagerState.currentPage)
    }

    LaunchedEffect(pillFilter) {
        val targetPage = pillFilters.indexOf(pillFilter)
        if (targetPage != -1 && targetPage != pillPagerState.currentPage) {
            pillPagerState.animateScrollToPage(targetPage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (workspace == AppWorkspace.TASKS) "NeverForget" else "NeverForget • Pills",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    // Theme Switcher Menu
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                imageVector = when (themeMode) {
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                },
                                contentDescription = "Theme: ${themeMode.displayName}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (mode) {
                                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                                ThemeMode.DARK -> Icons.Default.DarkMode
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Daily Summary Notification Settings Button (Tasks workspace only)
                    if (workspace == AppWorkspace.TASKS) {
                        IconButton(onClick = {
                            onRequestNotificationPermission()
                            showReminderSettings = true
                        }) {
                            Icon(
                                imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Daily Reminder Settings",
                                tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val tasksBadgeCount = stats.pendingToday + stats.overdueCount
                NavigationBarItem(
                    selected = workspace == AppWorkspace.TASKS,
                    onClick = { viewModel.setWorkspace(AppWorkspace.TASKS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (tasksBadgeCount > 0) {
                                    Badge { Text("$tasksBadgeCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Tasks"
                            )
                        }
                    },
                    label = { Text("Tasks") }
                )

                NavigationBarItem(
                    selected = workspace == AppWorkspace.PILLS,
                    onClick = { viewModel.setWorkspace(AppWorkspace.PILLS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pillStats.pendingDosesToday > 0) {
                                    Badge(containerColor = Color(0xFF00897B)) {
                                        Text("${pillStats.pendingDosesToday}", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = "Pills"
                            )
                        }
                    },
                    label = { Text("Pills") }
                )
            }
        },
        floatingActionButton = {
            if (workspace == AppWorkspace.TASKS) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            } else {
                FloatingActionButton(
                    onClick = {
                        onRequestNotificationPermission()
                        showAddPillDialog = true
                    },
                    containerColor = Color(0xFF00897B),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Pill")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (workspace == AppWorkspace.TASKS) {
                // Header Progress Card
                ProgressCard(
                    stats = stats,
                    today = today,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    onReminderClick = {
                        onRequestNotificationPermission()
                        showReminderSettings = true
                    }
                )

                // Filter Chips Scrollable Row (Synchronized auto-scrolls with pager)
                LazyRow(
                    state = filterListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { item ->
                        val isSelected = filter == item
                        val itemIndex = filters.indexOf(item)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setFilter(item)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(itemIndex)
                                }
                            },
                            label = {
                                val countText = when (item) {
                                    TaskFilter.TODAY_PENDING -> " (${stats.pendingToday})"
                                    TaskFilter.OVERDUE -> " (${stats.overdueCount})"
                                    TaskFilter.NEXT_7_DAYS -> " (${stats.pendingNext7Days})"
                                    TaskFilter.ALL -> " (${stats.totalAllTasks})"
                                    TaskFilter.TODAY_COMPLETED -> " (${stats.completedToday})"
                                    TaskFilter.RECURRING -> " (${stats.totalRecurringTasks})"
                                }
                                Text(text = "${item.label}$countText")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // HorizontalPager for swiping left/right between task tabs
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val currentTabFilter = filters[page]
                    val currentTabTasks = when (currentTabFilter) {
                        TaskFilter.TODAY_PENDING -> tasks.filter { it.isPendingOn(today) }
                        TaskFilter.OVERDUE -> tasks.filter { it.isOverdue(today) }
                        TaskFilter.NEXT_7_DAYS -> tasks.filter { it.isPendingInNext7Days(today) }
                        TaskFilter.ALL -> tasks
                        TaskFilter.TODAY_COMPLETED -> tasks.filter { it.isCompletedOn(today) }
                        TaskFilter.RECURRING -> tasks.filter { it.recurrence.isRecurring }
                    }

                    if (currentTabTasks.isEmpty()) {
                        EmptyState(filter = currentTabFilter, modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(
                                items = currentTabTasks,
                                key = { it.id }
                            ) { task ->
                                TaskCard(
                                    task = task,
                                    today = today,
                                    onToggle = { viewModel.toggleTask(task) },
                                    onEdit = { taskToEdit = task },
                                    onDelete = { viewModel.deleteTask(task) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Dedicated Pills Workspace
                PillProgressCard(
                    stats = pillStats,
                    today = today,
                    onTestPillAlert = {
                        onRequestNotificationPermission()
                        viewModel.testPillReminderNow()
                    }
                )

                LazyRow(
                    state = pillFilterListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pillFilters) { item ->
                        val isSelected = pillFilter == item
                        val itemIndex = pillFilters.indexOf(item)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setPillFilter(item)
                                coroutineScope.launch {
                                    pillPagerState.animateScrollToPage(itemIndex)
                                }
                            },
                            label = {
                                val countText = when (item) {
                                    PillFilter.TODAY_DOSES -> " (${pillStats.pendingDosesToday}/${pillStats.totalDosesToday})"
                                    PillFilter.ACTIVE_MEDS -> " (${pillStats.activeMedsCount})"
                                    PillFilter.FINISHED -> " (${pillStats.finishedMedsCount})"
                                }
                                Text(text = "${item.label}$countText")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalPager(
                    state = pillPagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val currentTab = pillFilters[page]
                    when (currentTab) {
                        PillFilter.TODAY_DOSES -> {
                            if (todayDoses.isEmpty()) {
                                PillEmptyState(filter = currentTab, modifier = Modifier.fillMaxSize())
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(
                                        items = todayDoses,
                                        key = { "${it.pill.id}_${it.timeSlot}" }
                                    ) { dose ->
                                        PillDoseCard(
                                            pill = dose.pill,
                                            timeSlot = dose.timeSlot,
                                            isTaken = dose.isTaken,
                                            today = today,
                                            onToggle = { viewModel.togglePillDose(dose.pill, dose.timeSlot) },
                                            onSnooze = { mins ->
                                                onRequestNotificationPermission()
                                                viewModel.snoozePillDose(dose.pill, dose.timeSlot, mins)
                                            },
                                            onEdit = { pillToEdit = dose.pill },
                                            onDelete = { viewModel.deletePill(dose.pill) }
                                        )
                                    }
                                }
                            }
                        }
                        PillFilter.ACTIVE_MEDS -> {
                            val activeMeds = allPills.filter { !it.isCourseCompleted(today) }
                            if (activeMeds.isEmpty()) {
                                PillEmptyState(filter = currentTab, modifier = Modifier.fillMaxSize())
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(
                                        items = activeMeds,
                                        key = { it.id }
                                    ) { pill ->
                                        MedicationCourseCard(
                                            pill = pill,
                                            today = today,
                                            onEdit = { pillToEdit = pill },
                                            onDelete = { viewModel.deletePill(pill) },
                                            onTestAlert = {
                                                onRequestNotificationPermission()
                                                viewModel.testPillReminderNow(pill)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        PillFilter.FINISHED -> {
                            val finishedMeds = allPills.filter { it.isCourseCompleted(today) }
                            if (finishedMeds.isEmpty()) {
                                PillEmptyState(filter = currentTab, modifier = Modifier.fillMaxSize())
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(
                                        items = finishedMeds,
                                        key = { it.id }
                                    ) { pill ->
                                        MedicationCourseCard(
                                            pill = pill,
                                            today = today,
                                            onEdit = { pillToEdit = pill },
                                            onDelete = { viewModel.deletePill(pill) },
                                            onTestAlert = {}
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddDialog) {
        AddEditTaskDialog(
            taskToEdit = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, description, recurrence, daysOfWeek, monthDay, startMonth, hour, minute ->
                val daysString = daysOfWeek.joinToString(",") { it.name }
                viewModel.addTask(
                    title = title,
                    description = description,
                    recurrence = recurrence,
                    recurrenceDaysOfWeek = daysString,
                    recurrenceMonthDay = monthDay,
                    recurrenceStartMonth = startMonth,
                    reminderHour = hour,
                    reminderMinute = minute
                )
                showAddDialog = false
            }
        )
    }

    // Edit Task Dialog
    if (taskToEdit != null) {
        AddEditTaskDialog(
            taskToEdit = taskToEdit,
            onDismiss = { taskToEdit = null },
            onSave = { title, description, recurrence, daysOfWeek, monthDay, startMonth, hour, minute ->
                taskToEdit?.let { existing ->
                    val daysString = daysOfWeek.joinToString(",") { it.name }
                    viewModel.updateTask(
                        existing.copy(
                            title = title,
                            description = description,
                            recurrence = recurrence,
                            recurrenceDaysOfWeek = daysString,
                            recurrenceMonthDay = monthDay,
                            recurrenceStartMonth = startMonth,
                            reminderHour = hour,
                            reminderMinute = minute
                        )
                    )
                }
                taskToEdit = null
            }
        )
    }

    val context = LocalContext.current

    // Reminder Settings & Backup Dialog
    if (showReminderSettings) {
        ReminderSettingsDialog(
            enabled = reminderEnabled,
            currentHour = reminderHour,
            currentMinute = reminderMinute,
            onDismiss = { showReminderSettings = false },
            onSave = { enabled, hour, minute ->
                viewModel.updateReminderSettings(enabled, hour, minute)
            },
            onTestNotification = {
                onRequestNotificationPermission()
                viewModel.testReminderNow()
            },
            onExportTasks = { uri ->
                viewModel.exportBackup(uri) { success ->
                    if (success) {
                        Toast.makeText(context, "Tasks exported successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to export tasks", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onImportTasks = { uri ->
                viewModel.importBackup(uri) { count ->
                    if (count >= 0) {
                        Toast.makeText(context, "Successfully restored $count task(s)!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to import tasks from backup file", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Task Notification Detail Dialog
    selectedNotificationTask?.let { task ->
        TaskNotificationDetailDialog(
            task = task,
            today = today,
            onDismiss = { viewModel.dismissTaskDetail() },
            onMarkDone = {
                viewModel.toggleTask(task)
                viewModel.dismissTaskDetail()
            },
            onSnooze = { hours ->
                viewModel.snoozeTask(task, hours)
            }
        )
    }

    // Add Pill Dialog
    if (showAddPillDialog) {
        AddEditPillDialog(
            pillToEdit = null,
            onDismiss = { showAddPillDialog = false },
            onSave = { name, dosage, notes, mealContext, doseTimes, durationDays ->
                viewModel.addPill(
                    name = name,
                    dosage = dosage,
                    notes = notes,
                    mealContext = mealContext,
                    doseTimes = doseTimes,
                    durationDays = durationDays
                )
                showAddPillDialog = false
            }
        )
    }

    // Edit Pill Dialog
    if (pillToEdit != null) {
        AddEditPillDialog(
            pillToEdit = pillToEdit,
            onDismiss = { pillToEdit = null },
            onSave = { name, dosage, notes, mealContext, doseTimes, durationDays ->
                pillToEdit?.let { existing ->
                    viewModel.updatePill(
                        existing.copy(
                            name = name,
                            dosage = dosage,
                            notes = notes,
                            mealContext = mealContext,
                            doseTimes = doseTimes.sorted().joinToString(","),
                            durationDays = durationDays
                        )
                    )
                }
                pillToEdit = null
            }
        )
    }

    // Pill Notification Detail Dialog
    selectedNotificationPill?.let { (pill, timeSlot) ->
        PillNotificationDetailDialog(
            pill = pill,
            timeSlot = timeSlot,
            today = today,
            onDismiss = { viewModel.dismissPillDetail() },
            onMarkTaken = {
                viewModel.togglePillDose(pill, timeSlot)
                viewModel.dismissPillDetail()
            },
            onSnooze = { minutes ->
                viewModel.snoozePillDose(pill, timeSlot, minutes)
            }
        )
    }
}
