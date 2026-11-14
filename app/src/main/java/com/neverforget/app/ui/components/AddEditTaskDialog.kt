package com.neverforget.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.neverforget.app.data.RecurrenceType
import com.neverforget.app.data.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskDialog(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        recurrence: RecurrenceType,
        daysOfWeek: Set<DayOfWeek>,
        monthDay: Int?,
        startMonth: Int?,
        reminderHour: Int?,
        reminderMinute: Int?
    ) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var recurrence by remember { mutableStateOf(taskToEdit?.recurrence ?: RecurrenceType.NONE) }
    var titleError by remember { mutableStateOf(false) }

    var selectedDaysOfWeek by remember {
        mutableStateOf(
            taskToEdit?.getSelectedDaysOfWeek()?.takeIf { it.isNotEmpty() }
                ?: setOf(LocalDate.now().dayOfWeek)
        )
    }

    var selectedMonthDay by remember {
        mutableIntStateOf(
            taskToEdit?.recurrenceMonthDay ?: LocalDate.now().dayOfMonth
        )
    }

    var selectedStartMonth by remember {
        mutableIntStateOf(
            taskToEdit?.recurrenceStartMonth ?: LocalDate.now().monthValue
        )
    }

    var hasSpecificTime by remember { mutableStateOf(taskToEdit?.reminderHour != null) }
    var selectedHour by remember { mutableIntStateOf(taskToEdit?.reminderHour ?: 9) }
    var selectedMinute by remember { mutableIntStateOf(taskToEdit?.reminderMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (taskToEdit == null) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError && it.isNotBlank()) titleError = false
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Read 20 pages, Doctor appointment") },
                    isError = titleError,
                    supportingText = {
                        if (titleError) {
                            Text("Title cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Description (Optional)") },
                    placeholder = { Text("Additional details or steps...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Specific Time / Reminder Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Specific Time Reminder",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Switch(
                        checked = hasSpecificTime,
                        onCheckedChange = { hasSpecificTime = it }
                    )
                }

                if (hasSpecificTime) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val timeFormatted = String.format(
                        java.util.Locale.getDefault(),
                        "%02d:%02d",
                        selectedHour,
                        selectedMinute
                    )

                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reminder Time: $timeFormatted",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Recurrence Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Repeat",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Recurrence Chips FlowRow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecurrenceType.entries.forEach { type ->
                        val isSelected = recurrence == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { recurrence = type },
                            label = { Text(type.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Custom Days Selection for Weekly Recurrence
                if (recurrence == RecurrenceType.WEEKLY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Days of Week",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val isDaySelected = selectedDaysOfWeek.contains(day)
                            val shortLabel = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                            FilterChip(
                                selected = isDaySelected,
                                onClick = {
                                    selectedDaysOfWeek = if (isDaySelected) {
                                        if (selectedDaysOfWeek.size > 1) selectedDaysOfWeek - day else selectedDaysOfWeek
                                    } else {
                                        selectedDaysOfWeek + day
                                    }
                                },
                                label = { Text(shortLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // Start Month Selection for Quarterly or Yearly Recurrence
                if (recurrence == RecurrenceType.QUARTERLY || recurrence == RecurrenceType.YEARLY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val monthName = Month.of(selectedStartMonth).name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    Text(
                        text = "Start Month: $monthName",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Month.entries.forEach { m ->
                            val isSelected = selectedStartMonth == m.value
                            val shortName = m.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStartMonth = m.value },
                                label = { Text(shortName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                }

                // Custom Date Selection for Monthly, Quarterly, or Yearly Recurrence
                if (recurrence == RecurrenceType.MONTHLY || recurrence == RecurrenceType.QUARTERLY || recurrence == RecurrenceType.YEARLY) {
                    val periodLabel = when (recurrence) {
                        RecurrenceType.QUARTERLY -> "every 3 months"
                        RecurrenceType.YEARLY -> "every year"
                        else -> "every month"
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Repeat on the ${selectedMonthDay}${getOrdinalSuffix(selectedMonthDay)} of $periodLabel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = selectedMonthDay.toFloat(),
                        onValueChange = { selectedMonthDay = it.toInt().coerceIn(1, 31) },
                        valueRange = 1f..31f,
                        steps = 29
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1, 15, 28, 31).forEach { presetDay ->
                            val isSelected = selectedMonthDay == presetDay
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMonthDay = presetDay },
                                label = {
                                    Text(
                                        if (presetDay == 31) "Last day (31st)"
                                        else "${presetDay}${getOrdinalSuffix(presetDay)}"
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                            } else {
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    recurrence,
                                    selectedDaysOfWeek,
                                    if (recurrence == RecurrenceType.MONTHLY || recurrence == RecurrenceType.QUARTERLY || recurrence == RecurrenceType.YEARLY) selectedMonthDay else null,
                                    if (recurrence == RecurrenceType.QUARTERLY || recurrence == RecurrenceType.YEARLY) selectedStartMonth else null,
                                    if (hasSpecificTime) selectedHour else null,
                                    if (hasSpecificTime) selectedMinute else null
                                )
                            }
                        }
                    ) {
                        Text(if (taskToEdit == null) "Create Task" else "Save Changes")
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        var isKeyboardMode by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isKeyboardMode) "Type Time (Numeric Keyboard)" else "Select Time (Clock)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { isKeyboardMode = !isKeyboardMode }) {
                        Icon(
                            imageVector = if (isKeyboardMode) Icons.Default.Schedule else Icons.Default.Keyboard,
                            contentDescription = if (isKeyboardMode) "Switch to Clock" else "Switch to Keyboard"
                        )
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isKeyboardMode) {
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }
                }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TimeInputPreview() {
    com.neverforget.app.ui.theme.NeverForgetTheme {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Type Time (Numeric Keyboard)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TimeInput(
                    state = rememberTimePickerState(
                        initialHour = 14,
                        initialMinute = 30,
                        is24Hour = true
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun AddEditTaskDialogWeeklyPreview() {
    com.neverforget.app.ui.theme.NeverForgetTheme {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Edit Task",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = "Car Insurance Renewal",
                    onValueChange = {},
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Start Month: May",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").forEach { month ->
                        FilterChip(
                            selected = month == "May",
                            onClick = {},
                            label = { Text(month) }
                        )
                    }
                }
            }
        }
    }
}
