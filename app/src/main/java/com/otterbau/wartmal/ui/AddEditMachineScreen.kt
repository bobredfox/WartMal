package com.otterbau.wartmal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

private const val MAX_FIELD_LENGTH = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMachineScreen(
    viewModel: MachineViewModel,
    machineId: String?,
    isEdit: Boolean
) {
    var name by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    LaunchedEffect(machineId) {
        if (isEdit && machineId != null) {
            viewModel.getMachine(machineId)?.let {
                name = it.name
                serialNumber = it.serialNumber
                location = it.location
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val nameValid = name.isNotBlank() && name.length <= MAX_FIELD_LENGTH
    val serialValid = serialNumber.length <= MAX_FIELD_LENGTH
    val locationValid = location.length <= MAX_FIELD_LENGTH
    val formValid = nameValid && serialValid && locationValid

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(if (isEdit) "Edit machine" else "New machine") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. CNC Milling Machine A1") },
                singleLine = true,
                isError = !nameValid,
                supportingText = { Text("${name.length}/$MAX_FIELD_LENGTH") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = serialNumber,
                onValueChange = { serialNumber = it },
                label = { Text("Serial number (optional)") },
                singleLine = true,
                isError = serialNumber.length > MAX_FIELD_LENGTH,
                supportingText = { Text("${serialNumber.length}/$MAX_FIELD_LENGTH") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (optional)") },
                placeholder = { Text("e.g. Workshop Floor 1 - Bay 3") },
                singleLine = true,
                isError = location.length > MAX_FIELD_LENGTH,
                supportingText = { Text("${location.length}/$MAX_FIELD_LENGTH") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (isEdit && machineId != null) {
                        val existing = viewModel.getMachine(machineId)
                        if (existing != null) {
                            viewModel.updateMachine(
                                existing, name.trim(), serialNumber.trim(), location.trim()
                            )
                        }
                    } else {
                        viewModel.addMachine(
                            name.trim(), serialNumber.trim(), location.trim()
                        )
                    }
                },
                enabled = formValid,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(if (isEdit) "Save changes" else "Add machine")
            }
        }
    }
}
