package com.otterbau.wartmal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.Check
import com.otterbau.wartmal.data.CheckStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineDetailScreen(
    viewModel: MachineViewModel,
    machineId: String
) {
    val machines by viewModel.machines.collectAsState()
    val machine = machines.find { it.id == machineId }

    var showAddCheck by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                        IconButton(onClick = { showDeleteConfirm = true }) {
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
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text("Machine not found.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Serial number",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = machine.serialNumber,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = machine.location,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Checks (${machine.checks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            if (machine.checks.isEmpty()) {
                item {
                    Text(
                        text = "No checks recorded. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(machine.checks, key = { it.id }) { check ->
                    CheckCard(
                        check = check,
                        onStatusChange = { status ->
                            viewModel.updateCheckStatus(machine.id, check.id, status)
                        },
                        onDelete = {
                            viewModel.deleteCheck(machine.id, check.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddCheck && machine != null) {
        AddCheckDialog(
            onDismiss = { showAddCheck = false },
            onConfirm = { title, description ->
                viewModel.addCheck(machine.id, title, description)
                showAddCheck = false
            }
        )
    }

    if (showDeleteConfirm && machine != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete machine") },
            text = { Text("Delete \"${machine.name}\" and all its checks?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMachine(machine.id)
                    showDeleteConfirm = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CheckCard(
    check: Check,
    onStatusChange: (CheckStatus) -> Unit,
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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = check.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete check")
                }
            }
            if (check.description.isNotBlank()) {
                Text(
                    text = check.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CheckStatus.values().forEach { status ->
                    FilledTonalButton(
                        onClick = { onStatusChange(status) },
                        enabled = check.status != status,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(status.label)
                    }
                }
            }
            Text(
                text = "Status: ${check.status.label}",
                style = MaterialTheme.typography.labelMedium,
                color = statusColor(check.status),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun statusColor(status: CheckStatus) = when (status) {
    CheckStatus.PASSED -> MaterialTheme.colorScheme.primary
    CheckStatus.FAILED -> MaterialTheme.colorScheme.error
    CheckStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun AddCheckDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add check") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), description.trim()) },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
