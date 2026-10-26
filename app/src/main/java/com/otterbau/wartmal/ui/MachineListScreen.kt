package com.otterbau.wartmal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.CheckSummary
import com.otterbau.wartmal.data.Machine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineListScreen(
    viewModel: MachineViewModel
) {
    val machines by viewModel.machines.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Machines") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.navigateToAddMachine() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add machine")
            }
        }
    ) { innerPadding ->
        if (machines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No machines yet.\nTap + to add one.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(machines, key = { it.id }) { machine ->
                    MachineCard(
                        machine = machine,
                        onClick = { viewModel.navigateToDetail(machine.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MachineCard(
    machine: Machine,
    onClick: () -> Unit
) {
    val summary = CheckSummary.from(machine.checks)

    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = machine.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "S/N: ${machine.serialNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = machine.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.total > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${summary.total} check${if (summary.total != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (summary.failed > 0) {
                        SummaryBadge(
                            icon = Icons.Filled.Error,
                            text = "${summary.failed} failed",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (summary.pending > 0) {
                        SummaryBadge(
                            icon = Icons.Filled.Schedule,
                            text = "${summary.pending} pending",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (summary.passed > 0) {
                        SummaryBadge(
                            icon = Icons.Filled.CheckCircle,
                            text = "${summary.passed} passed",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}
