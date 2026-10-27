package com.otterbau.wartmal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.Check
import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.RecurrenceInterval
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))

fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

fun relativeDueText(nextDueAt: Long, now: Long = System.currentTimeMillis()): String {
    val dayMillis = 86_400_000L
    val diffDays = ((nextDueAt - now) / dayMillis).toInt()
    return when {
        diffDays < 0 -> "Overdue ${-diffDays} day${if (-diffDays == 1) "" else "s"}"
        diffDays == 0 -> "Due today"
        diffDays == 1 -> "Due tomorrow"
        else -> "Due in $diffDays days"
    }
}

@Composable
fun StatusChip(status: CheckStatus) {
    val containerColor: Color
    val contentColor: Color
    val icon: ImageVector
    when (status) {
        CheckStatus.PASSED -> {
            containerColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            icon = Icons.Filled.CheckCircle
        }

        CheckStatus.FAILED -> {
            containerColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            icon = Icons.Filled.Error
        }

        CheckStatus.PENDING -> {
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            icon = Icons.Filled.Schedule
        }
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = status.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DueBadge(check: Check, now: Long = System.currentTimeMillis()) {
    val nextDueAt = check.nextDueAt ?: return
    val isOverdue = check.isOverdue(now)
    val isDueSoon = check.isDueSoon(now)
    if (!isOverdue && !isDueSoon) return

    val containerColor = if (isOverdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (isOverdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = relativeDueText(nextDueAt, now),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun RecurrenceBadge(recurrence: RecurrenceInterval) {
    if (recurrence == RecurrenceInterval.NONE) return
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Repeat,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = recurrence.label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrenceSelector(
    selected: RecurrenceInterval,
    onSelect: (RecurrenceInterval) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Repeat",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecurrenceInterval.entries.forEach { interval ->
                FilterChip(
                    selected = selected == interval,
                    onClick = { onSelect(interval) },
                    label = { Text(interval.label) }
                )
            }
        }
    }
}
