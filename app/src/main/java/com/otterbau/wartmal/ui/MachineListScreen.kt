package com.otterbau.wartmal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.CheckStatus
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
    val pendingCount = machine.checks.count { it.status == CheckStatus.PENDING }
    val failedCount = machine.checks.count { it.status == CheckStatus.FAILED }

    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxSize()
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
            val summary = buildString {
                append("${machine.checks.size} check${if (machine.checks.size != 1) "s" else ""}")
                if (pendingCount > 0) append("  -  $pendingCount pending")
                if (failedCount > 0) append("  -  $failedCount failed")
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
