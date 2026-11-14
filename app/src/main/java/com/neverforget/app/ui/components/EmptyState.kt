package com.neverforget.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neverforget.app.viewmodel.PillFilter
import com.neverforget.app.viewmodel.TaskFilter

@Composable
fun EmptyState(
    filter: TaskFilter,
    modifier: Modifier = Modifier
) {
    val (icon, title, message) = when (filter) {
        TaskFilter.TODAY_PENDING -> Triple(
            Icons.Default.CheckCircleOutline,
            "All Caught Up!",
            "You have no pending tasks for today. Great job staying on top of your goals!"
        )
        TaskFilter.OVERDUE -> Triple(
            Icons.Default.CheckCircleOutline,
            "No Overdue Tasks!",
            "You have no missed tasks from past dates. Excellent work staying on schedule!"
        )
        TaskFilter.NEXT_7_DAYS -> Triple(
            Icons.Default.CheckCircleOutline,
            "Next 7 Days Clear!",
            "You have no pending tasks scheduled for the next 7 days."
        )
        TaskFilter.TODAY_COMPLETED -> Triple(
            Icons.AutoMirrored.Filled.EventNote,
            "No Completed Tasks Yet",
            "Mark off your tasks as you complete them to see your progress here."
        )
        TaskFilter.RECURRING -> Triple(
            Icons.Default.Repeat,
            "No Recurring Tasks",
            "Set up daily, weekly, or weekday habits to stay consistent."
        )
        TaskFilter.ALL -> Triple(
            Icons.AutoMirrored.Filled.EventNote,
            "No Tasks Yet",
            "Tap the '+' button below to create your first task or habit."
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PillEmptyState(
    filter: PillFilter,
    modifier: Modifier = Modifier
) {
    val (icon, title, message) = when (filter) {
        PillFilter.TODAY_DOSES -> Triple(
            Icons.Default.Medication,
            "No Doses Today",
            "Tap the '+' button below to schedule a medication and its daily dose times."
        )
        PillFilter.ACTIVE_MEDS -> Triple(
            Icons.Default.Medication,
            "No Active Medications",
            "Add your medications with dosage, meal instructions, and daily reminder times."
        )
        PillFilter.FINISHED -> Triple(
            Icons.Default.CheckCircleOutline,
            "No Finished Courses",
            "Completed treatment courses (e.g. 7-day or 14-day courses) will appear here."
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF00897B).copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
