package com.otterbau.wartmal.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.GeoPoint
import java.util.Locale

private const val MAX_FIELD_LENGTH = 120

private fun parseCoordinate(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()

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
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }

    val devicePosition by viewModel.devicePosition.collectAsState()
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsState()
    val hasLocationPermission = locationPermissionGranted

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.refreshLocationPermission()
        }
    }

    LaunchedEffect(machineId) {
        if (isEdit && machineId != null) {
            viewModel.getMachine(machineId)?.let {
                name = it.name
                serialNumber = it.serialNumber
                location = it.location
                latitudeText = it.locationPoint?.let { p -> formatCoordinate(p.latitude) } ?: ""
                longitudeText = it.locationPoint?.let { p -> formatCoordinate(p.longitude) } ?: ""
            }
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            viewModel.startLocationTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopLocationTracking() }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val nameValid = name.isNotBlank() && name.length <= MAX_FIELD_LENGTH
    val serialValid = serialNumber.length <= MAX_FIELD_LENGTH
    val locationValid = location.length <= MAX_FIELD_LENGTH

    val latitudeValue = parseCoordinate(latitudeText)
    val longitudeValue = parseCoordinate(longitudeText)
    val coordinatesBlank = latitudeText.isBlank() && longitudeText.isBlank()
    val coordinatesValid = coordinatesBlank ||
        (latitudeValue != null && longitudeValue != null &&
            latitudeValue in -90.0..90.0 && longitudeValue in -180.0..180.0)
    val coordinatesError = !coordinatesBlank && !coordinatesValid

    val locationPoint = when {
        latitudeValue != null && longitudeValue != null ->
            runCatching { GeoPoint(latitudeValue, longitudeValue) }.getOrNull()

        else -> null
    }
    val formValid = nameValid && serialValid && locationValid && coordinatesValid

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

            Text(
                text = "GPS coordinates (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("Latitude") },
                    placeholder = { Text("52.5200") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = coordinatesError,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("Longitude") },
                    placeholder = { Text("13.4050") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = coordinatesError,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (coordinatesError) {
                Text(
                    text = "Enter both latitude (-90 to 90) and longitude (-180 to 180).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (hasLocationPermission) {
                            viewModel.startLocationTracking()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        val current = devicePosition?.point
                        if (current != null) {
                            latitudeText = formatCoordinate(current.latitude)
                            longitudeText = formatCoordinate(current.longitude)
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use current location")
                }
                OutlinedButton(
                    onClick = {
                        latitudeText = ""
                        longitudeText = ""
                    },
                    enabled = !coordinatesBlank,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
            if (devicePosition != null && hasLocationPermission) {
                Text(
                    text = "Device position: " +
                        "${formatCoordinate(devicePosition.point.latitude)}, " +
                        formatCoordinate(devicePosition.point.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    if (isEdit && machineId != null) {
                        val existing = viewModel.getMachine(machineId)
                        if (existing != null) {
                            viewModel.updateMachine(
                                existing,
                                name.trim(),
                                serialNumber.trim(),
                                location.trim(),
                                locationPoint
                            )
                        }
                    } else {
                        viewModel.addMachine(
                            name.trim(),
                            serialNumber.trim(),
                            location.trim(),
                            locationPoint
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

private fun formatCoordinate(value: Double): String =
    String.format(Locale.US, "%.6f", value)
