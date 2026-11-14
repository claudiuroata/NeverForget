package com.neverforget.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neverforget.app.viewmodel.TaskStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressCard(
    stats: TaskStats,
    today: LocalDate,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onReminderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = stats.progress,
        label = "progress_animation"
    )

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    val formattedDate = today.format(dateFormatter)

    val timeFormatted = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        reminderHour,
        reminderMinute
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header Row: Streak Badge & Reminder Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "🔥 Daily Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Clickable reminder pill
                    Surface(
                        onClick = onReminderClick,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = "Daily Reminder",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Text(
                                text = if (reminderEnabled) timeFormatted else "Off",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Middle Content: Date, Status, and Circular Gauge Ring
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (stats.totalDueToday == 0) {
                                "No tasks scheduled for today"
                            } else if (stats.pendingToday == 0) {
                                "All tasks completed! 🎉"
                            } else {
                                "${stats.completedToday} of ${stats.totalDueToday} completed"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Circular Progress Gauge Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(68.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White,
                            strokeWidth = 6.dp,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ProgressCardPreview() {
    com.neverforget.app.ui.theme.NeverForgetTheme {
        ProgressCard(
            stats = TaskStats(totalDueToday = 5, completedToday = 3, pendingToday = 2, progress = 0.6f),
            today = LocalDate.now(),
            reminderEnabled = true,
            reminderHour = 9,
            reminderMinute = 0,
            onReminderClick = {}
        )
    }
}
