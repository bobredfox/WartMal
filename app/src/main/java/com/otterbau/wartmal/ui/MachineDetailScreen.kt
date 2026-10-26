package com.otterbau.wartmal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.Check
import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.CheckSummary
import com.otterbau.wartmal.data.sortedForDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_FIELD_LENGTH = 120
private const val MAX_DESCRIPTION_LENGTH = 1000
private const val MAX_NOTE_LENGTH = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineDetailScreen(
    viewModel: MachineViewModel,
    machineId: String
) {
    val machines by viewModel.machines.collectAsState()
    val machine = machines.find { it.id == machineId }

    var showAddCheck by remember { mutableStateOf(false) }
    var showDeleteMachine by remember { mutableStateOf(false) }
    var checkFilter by remember { mutableStateOf<CheckStatus?>(null) }
    var editingCheck by remember { mutableStateOf<Check?>(null) }
    var completeTarget by remember { mutableStateOf<Pair<Check, CheckStatus>?>(null) }
    var deletingCheck by remember { mutableStateOf<Check?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(machine?.name ?: "Machine") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (machine != null) {
                        IconButton(onClick = { viewModel.navigateToEditMachine(machine.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit machine")
                        }
                        IconButton(onClick = { showDeleteMachine = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete machine")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (machine != null) {
                FloatingActionButton(onClick = { showAddCheck = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add check")
                }
            }
        }
    ) { innerPadding ->
        if (machine == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Machine not found.")
            }
            return@Scaffold
        }

        val visibleChecks = machine.checks
            .sortedForDisplay()
            .filter { checkFilter == null || it.status == checkFilter }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Serial number",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = machine.serialNumber.ifBlank { "-" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = machine.location.ifBlank { "-" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Checks (${machine.checks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    CheckSummaryRow(summary = CheckSummary.from(machine.checks))
                }
            }

            if (machine.checks.isNotEmpty()) {
                item {
                    CheckFilterRow(
                        selected = checkFilter,
                        onSelect = { checkFilter = it }
                    )
                }
            }

            if (visibleChecks.isEmpty()) {
                item {
                    Text(
                        text = if (machine.checks.isEmpty()) {
                            "No checks recorded. Tap + to add one."
                        } else {
                            "No checks with this status."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(visibleChecks, key = { it.id }) { check ->
                    CheckCard(
                        check = check,
                        onComplete = { status -> completeTarget = check to status },
                        onReset = {
                            viewModel.updateCheckStatus(machine.id, check.id, CheckStatus.PENDING)
                        },
                        onEdit = { editingCheck = check },
                        onDelete = { deletingCheck = check }
                    )
                }
            }
        }
    }

    if (showAddCheck && machine != null) {
        CheckFormDialog(
            dialogTitle = "Add check",
            initialTitle = "",
            initialDescription = "",
            confirmLabel = "Add",
            onDismiss = { showAddCheck = false },
            onConfirm = { title, description ->
                viewModel.addCheck(machine.id, title, description)
                showAddCheck = false
            }
        )
    }

    editingCheck?.let { check ->
        if (machine != null) {
            CheckFormDialog(
                dialogTitle = "Edit check",
                initialTitle = check.title,
                initialDescription = check.description,
                confirmLabel = "Save",
                onDismiss = { editingCheck = null },
                onConfirm = { title, description ->
                    viewModel.updateCheck(machine.id, check.id, title, description)
                    editingCheck = null
                }
            )
        }
    }

    completeTarget?.let { (check, status) ->
        if (machine != null) {
            CompleteCheckDialog(
                check = check,
                target = status,
                onDismiss = { completeTarget = null },
                onConfirm = { note ->
                    viewModel.updateCheckStatus(machine.id, check.id, status, note)
                    completeTarget = null
                }
            )
        }
    }

    deletingCheck?.let { check ->
        if (machine != null) {
            AlertDialog(
                onDismissRequest = { deletingCheck = null },
                title = { Text("Delete check") },
                text = { Text("Delete \"${check.title}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCheck(machine.id, check.id)
                        deletingCheck = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingCheck = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (showDeleteMachine && machine != null) {
        AlertDialog(
            onDismissRequest = { showDeleteMachine = false },
            title = { Text("Delete machine") },
            text = { Text("Delete \"${machine.name}\" and all its checks?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMachine(machine.id)
                    showDeleteMachine = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMachine = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CheckSummaryRow(summary: CheckSummary) {
    if (summary.total == 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summary.failed > 0) {
            SummaryItem(text = "${summary.failed} failed", color = MaterialTheme.colorScheme.error)
        }
        if (summary.pending > 0) {
            SummaryItem(
                text = "${summary.pending} pending",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (summary.passed > 0) {
            SummaryItem(text = "${summary.passed} passed", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryItem(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}

@Composable
private fun CheckFilterRow(
    selected: CheckStatus?,
    onSelect: (CheckStatus?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") }
            )
        }
        items(CheckStatus.values()) { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(status.label) }
            )
        }
    }
}

@Composable
private fun CheckCard(
    check: Check,
    onComplete: (CheckStatus) -> Unit,
    onReset: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = check.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(status = check.status)
            }
            if (check.description.isNotBlank()) {
                Text(
                    text = check.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (check.note.isNotBlank()) {
                Text(
                    text = "Result note",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = check.note,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = check.performedAt?.let { "Performed: ${formatTimestamp(it)}" }
                        ?: "Not performed yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit check")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete check")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onComplete(CheckStatus.PASSED) },
                    enabled = check.status != CheckStatus.PASSED,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Passed", modifier = Modifier.padding(start = 4.dp))
                }
                FilledTonalButton(
                    onClick = { onComplete(CheckStatus.FAILED) },
                    enabled = check.status != CheckStatus.FAILED,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Failed", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = check.status != CheckStatus.PENDING,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Reset", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: CheckStatus) {
    val containerColor: Color
    val contentColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = status.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CheckFormDialog(
    dialogTitle: String,
    initialTitle: String,
    initialDescription: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    val titleValid = title.isNotBlank() && title.length <= MAX_FIELD_LENGTH
    val descriptionValid = description.length <= MAX_DESCRIPTION_LENGTH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    isError = title.length > MAX_FIELD_LENGTH,
                    supportingText = { Text("${title.length}/$MAX_FIELD_LENGTH") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 5,
                    isError = description.length > MAX_DESCRIPTION_LENGTH,
                    supportingText = { Text("${description.length}/$MAX_DESCRIPTION_LENGTH") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), description.trim()) },
                enabled = titleValid && descriptionValid
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CompleteCheckDialog(
    check: Check,
    target: CheckStatus,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (target == CheckStatus.PASSED) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null
            )
        },
        title = { Text("Mark as ${target.label.lowercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Record the result for \"${check.title}\".",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Result note (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    isError = note.length > MAX_NOTE_LENGTH,
                    supportingText = { Text("${note.length}/$MAX_NOTE_LENGTH") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(note.trim()) },
                enabled = note.length <= MAX_NOTE_LENGTH
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
