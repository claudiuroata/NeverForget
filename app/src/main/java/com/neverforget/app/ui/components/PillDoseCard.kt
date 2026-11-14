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
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.unit.dp
import com.neverforget.app.data.MealContext
import com.neverforget.app.data.Pill
import com.neverforget.app.ui.theme.AmberWarning
import com.neverforget.app.ui.theme.EmeraldSuccess
import java.time.LocalDate

private val PillTeal = Color(0xFF00897B)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PillDoseCard(
    pill: Pill,
    timeSlot: String,
    isTaken: Boolean,
    today: LocalDate,
    onToggle: () -> Unit,
    onSnooze: (minutes: Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val activeAccentColor = if (isTaken) Color.Gray.copy(alpha = 0.35f) else PillTeal

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
                drawRect(
                    color = activeAccentColor,
                    topLeft = Offset.Zero,
                    size = Size(width = 6.dp.toPx(), height = size.height)
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isTaken) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTaken) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTaken,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldSuccess,
                    uncheckedColor = PillTeal
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = pill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isTaken) FontWeight.Normal else FontWeight.Bold,
                        textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isTaken) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "• ${pill.dosage}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isTaken) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        } else {
                            PillTeal
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (pill.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = pill.notes,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Time slot badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PillTeal.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Dose Time",
                                modifier = Modifier.size(12.dp),
                                tint = PillTeal
                            )
                            Text(
                                text = timeSlot,
                                style = MaterialTheme.typography.labelSmall,
                                color = PillTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Meal instruction badge
                    if (pill.mealContext != MealContext.ANYTIME) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = "Meal Instruction",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = pill.mealContext.badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Course progress badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = pill.getCourseProgressText(today),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Snooze status
                    pill.getFormattedSnoozeText(today, timeSlot)?.let { snoozeText ->
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
                                    text = snoozeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isTaken) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Taken ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Dose Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isTaken) {
                        DropdownMenuItem(
                            text = { Text("Snooze 15m") },
                            leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSnooze(15)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze 30m") },
                            leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSnooze(30)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze 1h") },
                            leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSnooze(60)
                            }
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("Edit Medication") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Medication", color = MaterialTheme.colorScheme.error) },
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
