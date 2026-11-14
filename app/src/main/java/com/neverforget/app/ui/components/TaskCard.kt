package com.neverforget.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neverforget.app.data.RecurrenceType
import com.neverforget.app.data.Task
import com.neverforget.app.ui.theme.AmberWarning
import com.neverforget.app.ui.theme.EmeraldSuccess
import com.neverforget.app.ui.theme.PurpleRecurrence
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: Task,
    today: LocalDate,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = task.isCompletedOn(today)
    var showMenu by remember { mutableStateOf(false) }

    val accentColor = when (task.recurrence) {
        RecurrenceType.NONE -> MaterialTheme.colorScheme.primary
        RecurrenceType.DAILY -> EmeraldSuccess
        RecurrenceType.WEEKDAYS -> MaterialTheme.colorScheme.secondary
        RecurrenceType.WEEKLY -> PurpleRecurrence
        RecurrenceType.MONTHLY -> AmberWarning
        RecurrenceType.QUARTERLY -> Color(0xFF00897B)
        RecurrenceType.YEARLY -> Color(0xFFD81B60)
    }

    val activeAccentColor = if (isCompleted) Color.Gray.copy(alpha = 0.35f) else accentColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .combinedClickable(
                onClick = onEdit,
                onLongClick = { showMenu = true }
            )
            .drawWithContent {
                drawContent()
                // Draw 6dp left category color accent strip matching card height dynamically
                drawRect(
                    color = activeAccentColor,
                    topLeft = Offset.Zero,
                    size = Size(width = 6.dp.toPx(), height = size.height)
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldSuccess,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Task Content Column (unlocked height: expands naturally to fit all rows of tags)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Bold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }

                // Badges FlowRow (unlocked: wraps tags across multiple rows without clipping)
                val snoozeText = task.getFormattedSnoozeText()
                if (task.recurrence.isRecurring || task.reminderHour != null || snoozeText != null || isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        snoozeText?.let { text ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberWarning.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Snooze,
                                        contentDescription = "Snoozed",
                                        modifier = Modifier.size(12.dp),
                                        tint = AmberWarning
                                    )
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        task.getFormattedTimeText()?.let { timeText ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "Scheduled Time",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (task.recurrence.isRecurring) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Recurring",
                                        modifier = Modifier.size(12.dp),
                                        tint = PurpleRecurrence
                                    )
                                    Text(
                                        text = task.getFormattedRecurrenceText(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PurpleRecurrence,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isCompleted && task.recurrence.isRecurring) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Done for today",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Overflow / Options Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Task Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Task") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Task", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Light Theme")
@Composable
fun TaskCardPreview() {
    com.neverforget.app.ui.theme.NeverForgetTheme(themeMode = com.neverforget.app.ui.theme.ThemeMode.LIGHT) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TaskCard(
                task = Task(
                    id = 1,
                    title = "Gym Workout",
                    description = "Leg day session",
                    recurrence = RecurrenceType.WEEKLY,
                    recurrenceDaysOfWeek = "MONDAY,WEDNESDAY,FRIDAY"
                ),
                today = LocalDate.now(),
                onToggle = {},
                onEdit = {},
                onDelete = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            TaskCard(
                task = Task(
                    id = 2,
                    title = "Pay Internet Bill",
                    description = "Monthly utility payment",
                    recurrence = RecurrenceType.MONTHLY,
                    recurrenceMonthDay = 15
                ),
                today = LocalDate.now(),
                onToggle = {},
                onEdit = {},
                onDelete = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Dark Theme")
@Composable
fun TaskCardDarkPreview() {
    com.neverforget.app.ui.theme.NeverForgetTheme(themeMode = com.neverforget.app.ui.theme.ThemeMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                TaskCard(
                    task = Task(
                        id = 1,
                        title = "Gym Workout (Mon, Wed, Fri)",
                        description = "Snoozed task preview with multiple rows of tags",
                        recurrence = RecurrenceType.WEEKLY,
                        recurrenceDaysOfWeek = "MONDAY,WEDNESDAY,FRIDAY",
                        reminderHour = 14,
                        reminderMinute = 30,
                        snoozedUntil = System.currentTimeMillis() + 3600000
                    ),
                    today = LocalDate.now(),
                    onToggle = {},
                    onEdit = {},
                    onDelete = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
                TaskCard(
                    task = Task(
                        id = 2,
                        title = "Pay Credit Card Bill",
                        description = "Due on 15th of every month",
                        recurrence = RecurrenceType.MONTHLY,
                        recurrenceMonthDay = 15,
                        isCompleted = true,
                        lastCompletedDate = LocalDate.now().toString()
                    ),
                    today = LocalDate.now(),
                    onToggle = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }
    }
}
