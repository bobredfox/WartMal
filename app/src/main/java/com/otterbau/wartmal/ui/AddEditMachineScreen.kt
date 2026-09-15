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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Machine" else "Add Machine") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = serialNumber,
                onValueChange = { serialNumber = it },
                label = { Text("Serial number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (name.isNotBlank()) {
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
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEdit) "Save" else "Add")
            }
        }
    }
}
