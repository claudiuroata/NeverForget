package com.neverforget.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.neverforget.app.data.MealContext
import com.neverforget.app.data.Pill
import java.util.Locale

private val PillTeal = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPillDialog(
    pillToEdit: Pill? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        dosage: String,
        notes: String,
        mealContext: MealContext,
        doseTimes: List<String>,
        durationDays: Int?
    ) -> Unit
) {
    var name by remember { mutableStateOf(pillToEdit?.name ?: "") }
    var dosage by remember { mutableStateOf(pillToEdit?.dosage ?: "1 pill") }
    var notes by remember { mutableStateOf(pillToEdit?.notes ?: "") }
    var mealContext by remember { mutableStateOf(pillToEdit?.mealContext ?: MealContext.ANYTIME) }
    var doseTimes by remember {
        mutableStateOf(
            pillToEdit?.getScheduledTimes()?.takeIf { it.isNotEmpty() } ?: listOf("08:00")
        )
    }

    var isFixedCourse by remember { mutableStateOf(pillToEdit?.durationDays != null) }
    var durationDaysText by remember {
        mutableStateOf((pillToEdit?.durationDays ?: 7).toString())
    }

    var nameError by remember { mutableStateOf(false) }
    var timesError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = PillTeal
                    )
                    Text(
                        text = if (pillToEdit == null) "New Medication" else "Edit Medication",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Medication Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && it.isNotBlank()) nameError = false
                    },
                    label = { Text("Medication Name *") },
                    placeholder = { Text("e.g. Vitamin D3, Augmentin") },
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text("Name cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dosage
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") },
                    placeholder = { Text("e.g. 1 pill, 500mg, 2 drops") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Drink a full glass of water") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Meal Instructions Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PillTeal
                    )
                    Text(
                        text = "Meal Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MealContext.entries.forEach { contextOption ->
                        val isSelected = mealContext == contextOption
                        FilterChip(
                            selected = isSelected,
                            onClick = { mealContext = contextOption },
                            label = { Text(contextOption.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Daily Dose Times Section
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
                            tint = PillTeal
                        )
                        Text(
                            text = "Daily Dose Times",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(onClick = { showTimePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Time")
                    }
                }

                if (timesError) {
                    Text(
                        text = "Please keep at least one dose time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Current Selected Time Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    doseTimes.forEach { timeSlot ->
                        InputChip(
                            selected = true,
                            onClick = {
                                if (doseTimes.size > 1) {
                                    doseTimes = doseTimes.filter { it != timeSlot }
                                }
                            },
                            label = {
                                Text(
                                    text = timeSlot,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            trailingIcon = {
                                if (doseTimes.size > 1) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove $timeSlot",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Preset Dose Times
                Text(
                    text = "Quick toggle presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("08:00", "14:00", "20:00").forEach { preset ->
                        val active = preset in doseTimes
                        FilterChip(
                            selected = active,
                            onClick = {
                                if (active) {
                                    if (doseTimes.size > 1) {
                                        doseTimes = doseTimes.filter { it != preset }
                                    }
                                } else {
                                    doseTimes = (doseTimes + preset).distinct().sorted()
                                    timesError = false
                                }
                            },
                            label = { Text(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Treatment Duration Section
                Text(
                    text = "Treatment Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !isFixedCourse,
                        onClick = {
                            isFixedCourse = false
                            durationError = false
                        },
                        label = { Text("Ongoing") }
                    )
                    FilterChip(
                        selected = isFixedCourse,
                        onClick = { isFixedCourse = true },
                        label = { Text("Fixed Course (Days)") }
                    )
                }

                if (isFixedCourse) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(5, 7, 10, 14, 30).forEach { presetDays ->
                            val isSelected = durationDaysText.toIntOrNull() == presetDays
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    durationDaysText = presetDays.toString()
                                    durationError = false
                                },
                                label = { Text("${presetDays}d") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = durationDaysText,
                        onValueChange = { input ->
                            durationDaysText = input.filter { it.isDigit() }
                            if (durationError && (durationDaysText.toIntOrNull() ?: 0) > 0) {
                                durationError = false
                            }
                        },
                        label = { Text("Number of days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = durationError,
                        supportingText = {
                            if (durationError) {
                                Text("Enter a valid number of days (> 0)", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            val parsedDuration = if (isFixedCourse) durationDaysText.toIntOrNull() else null
                            var valid = true
                            if (name.isBlank()) {
                                nameError = true
                                valid = false
                            }
                            if (doseTimes.isEmpty()) {
                                timesError = true
                                valid = false
                            }
                            if (isFixedCourse && (parsedDuration == null || parsedDuration <= 0)) {
                                durationError = true
                                valid = false
                            }
                            if (valid) {
                                onSave(
                                    name.trim(),
                                    dosage.trim().ifBlank { "1 pill" },
                                    notes.trim(),
                                    mealContext,
                                    doseTimes,
                                    parsedDuration
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PillTeal)
                    ) {
                        Text(if (pillToEdit == null) "Save Medication" else "Save Changes")
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = true
        )
        var isKeyboardMode by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        doseTimes = (doseTimes + formatted).distinct().sorted()
                        timesError = false
                        showTimePicker = false
                    }
                ) {
                    Text("Add")
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
                        text = if (isKeyboardMode) "Type Dose Time" else "Select Dose Time",
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
