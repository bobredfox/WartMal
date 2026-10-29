package com.otterbau.wartmal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.Check
import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.CheckSummary
import com.otterbau.wartmal.data.Machine
import com.otterbau.wartmal.data.MaintenanceHistoryEntry
import com.otterbau.wartmal.data.RecurrenceInterval
import com.otterbau.wartmal.data.sortedForDisplay
import java.util.Locale

private const val MAX_FIELD_LENGTH = 120
private const val MAX_DESCRIPTION_LENGTH = 1000
private const val MAX_NOTE_LENGTH = 1000

private enum class DetailTab(val label: String) {
    Checks("Maintenance"),
    History("History")
}

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
    var selectedTab by remember { mutableIntStateOf(0) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = machine?.name ?: "Machine",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (machine != null) {
                            Text(
                                text = machine.location.ifBlank { machine.serialNumber },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (machine != null) {
                        if (machine.locationPoint != null) {
                            IconButton(onClick = { viewModel.navigateToFindMachine(machine.id) }) {
                                Icon(Icons.Filled.Navigation, contentDescription = "Find machine")
                            }
                        }
                        IconButton(onClick = { viewModel.navigateToEditMachine(machine.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit machine")
                        }
                        IconButton(onClick = { showDeleteMachine = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete machine")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            if (machine != null && selectedTab == DetailTab.Checks.ordinal) {
                FilledTonalButton(onClick = { showAddCheck = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New check")
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                DetailTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label) }
                    )
                }
            }

            when (DetailTab.entries[selectedTab]) {
                DetailTab.Checks -> {
                    ChecksTab(
                        machine = machine,
                        checkFilter = checkFilter,
                        onFilterChange = { checkFilter = it },
                        onComplete = { check, status -> completeTarget = check to status },
                        onReset = {
                            viewModel.updateCheckStatus(machine.id, it.id, CheckStatus.PENDING)
                        },
                        onEdit = { editingCheck = it },
                        onDelete = { deletingCheck = it },
                        onFind = { viewModel.navigateToFindMachine(it.id) }
                    )
                }

                DetailTab.History -> {
                    HistoryTab(history = machine.history)
                }
            }
        }
    }

    if (showAddCheck && machine != null) {
        CheckFormDialog(
            dialogTitle = "New maintenance check",
            initialTitle = "",
            initialDescription = "",
            initialRecurrence = RecurrenceInterval.NONE,
            confirmLabel = "Add",
            onDismiss = { showAddCheck = false },
            onConfirm = { title, description, recurrence ->
                viewModel.addCheck(machine.id, title, description, recurrence)
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
                initialRecurrence = check.recurrence,
                confirmLabel = "Save",
                onDismiss = { editingCheck = null },
                onConfirm = { title, description, recurrence ->
                    viewModel.updateCheck(machine.id, check.id, title, description, recurrence)
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
private fun ChecksTab(
    machine: Machine,
    checkFilter: CheckStatus?,
    onFilterChange: (CheckStatus?) -> Unit,
    onComplete: (Check, CheckStatus) -> Unit,
    onReset: (Check) -> Unit,
    onEdit: (Check) -> Unit,
    onDelete: (Check) -> Unit,
    onFind: (Machine) -> Unit
) {
    val visibleChecks = machine.checks
        .sortedForDisplay()
        .filter { checkFilter == null || it.status == checkFilter }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        item {
            MachineInfoCard(machine = machine, onFind = onFind)
        }

        if (machine.checks.isNotEmpty()) {
            item {
                CheckFilterRow(selected = checkFilter, onSelect = onFilterChange)
            }
        }

        if (visibleChecks.isEmpty()) {
            item {
                Text(
                    text = if (machine.checks.isEmpty()) {
                        "No maintenance checks yet. Tap \"New check\" to add one."
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
                    onComplete = { status -> onComplete(check, status) },
                    onReset = { onReset(check) },
                    onEdit = { onEdit(check) },
                    onDelete = { onDelete(check) }
                )
            }
        }
    }
}

@Composable
private fun MachineInfoCard(machine: Machine, onFind: (Machine) -> Unit = {}) {
    val summary = CheckSummary.from(machine.checks)
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = "Serial number",
                    value = machine.serialNumber.ifBlank { "-" }
                )
                InfoItem(
                    label = "Location",
                    value = machine.location.ifBlank { "-" }
                )
            }
            if (machine.locationPoint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoItem(
                        label = "GPS",
                        value = String.format(
                            Locale.US,
                            "%.5f, %.5f",
                            machine.locationPoint.latitude,
                            machine.locationPoint.longitude
                        )
                    )
                    FilledTonalButton(onClick = { onFind(machine) }) {
                        Icon(
                            Icons.Filled.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Find")
                    }
                }
            }
            if (summary.total > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${summary.total} checks",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (summary.failed > 0) {
                        Text(
                            text = "${summary.failed} failed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (summary.pending > 0) {
                        Text(
                            text = "${summary.pending} pending",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (summary.passed > 0) {
                        Text(
                            text = "${summary.passed} passed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun HistoryTab(history: List<MaintenanceHistoryEntry>) {
    if (history.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No maintenance history yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Completed checks will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        items(history, key = { it.id }) { entry ->
            HistoryCard(entry = entry)
        }
    }
}

@Composable
private fun HistoryCard(entry: MaintenanceHistoryEntry) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (entry.status == CheckStatus.PASSED) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Error
                    },
                    contentDescription = null,
                    tint = if (entry.status == CheckStatus.PASSED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = entry.checkTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDateTime(entry.performedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.note.isNotBlank()) {
                        Text(
                            text = entry.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status = entry.status)
                if (entry.recurrence != RecurrenceInterval.NONE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    RecurrenceBadge(recurrence = entry.recurrence)
                }
            }
        }
    }
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
        items(CheckStatus.entries) { status ->
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (check.isOverdue()) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = check.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
            if (check.note.isNotBlank() && check.status != CheckStatus.PENDING) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (check.recurrence != RecurrenceInterval.NONE) {
                    RecurrenceBadge(recurrence = check.recurrence)
                }
                DueBadge(check = check)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        check.nextDueAt != null && check.status == CheckStatus.PENDING ->
                            "Due: ${formatDate(check.nextDueAt)}"

                        check.performedAt != null -> "Performed: ${formatDate(check.performedAt)}"
                        else -> "Not performed yet"
                    },
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Passed")
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Failed")
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = check.status != CheckStatus.PENDING,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun CheckFormDialog(
    dialogTitle: String,
    initialTitle: String,
    initialDescription: String,
    initialRecurrence: RecurrenceInterval,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, recurrence: RecurrenceInterval) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var recurrence by remember { mutableStateOf(initialRecurrence) }

    val titleValid = title.isNotBlank() && title.length <= MAX_FIELD_LENGTH
    val descriptionValid = description.length <= MAX_DESCRIPTION_LENGTH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                RecurrenceSelector(
                    selected = recurrence,
                    onSelect = { recurrence = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), description.trim(), recurrence) },
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
    val isRecurring = check.recurrence != RecurrenceInterval.NONE

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (target == CheckStatus.PASSED) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Error
                },
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
                if (isRecurring) {
                    Text(
                        text = "This check repeats ${check.recurrence.label.lowercase()}. " +
                            "A new due date will be scheduled automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
