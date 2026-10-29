package com.otterbau.wartmal.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.otterbau.wartmal.data.GeoPoint
import com.otterbau.wartmal.data.Machine
import com.otterbau.wartmal.data.bearingToDirection
import com.otterbau.wartmal.data.formatDistance
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindMachineScreen(
    viewModel: MachineViewModel,
    machineId: String
) {
    val machines by viewModel.machines.collectAsState()
    val machine = machines.find { it.id == machineId }

    val devicePosition by viewModel.devicePosition.collectAsState()
    val deviceAzimuth by viewModel.deviceAzimuth.collectAsState()
    val hasPermission by viewModel.locationPermissionGranted.collectAsState()

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.refreshLocationPermission()
        }
    }

    DisposableEffect(machineId) {
        viewModel.startLocationTracking()
        onDispose { viewModel.stopLocationTracking() }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Find machine")
                        if (machine != null) {
                            Text(
                                text = machine.name,
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
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        when {
            machine == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Machine not found.")
                }
            }

            machine.locationPoint == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No GPS coordinates saved",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Add GPS coordinates to \"${machine.name}\" to use the find function.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.navigateToEditMachine(machine.id) }) {
                        Text("Edit machine")
                    }
                }
            }

            else -> {
                FindMachineContent(
                    machine = machine,
                    target = machine.locationPoint,
                    devicePosition = devicePosition?.point,
                    deviceAccuracyMeters = devicePosition?.accuracyMeters,
                    deviceAzimuth = deviceAzimuth,
                    hasPermission = hasPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onOpenInMaps = {
                        val point = machine.locationPoint
                        if (point != null) {
                            val uri = Uri.parse(
                                String.format(
                                    Locale.US,
                                    "geo:%f,%f?q=%f,%f(%s)",
                                    point.latitude,
                                    point.longitude,
                                    point.latitude,
                                    point.longitude,
                                    Uri.encode(machine.name)
                                )
                            )
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun FindMachineContent(
    machine: Machine,
    target: GeoPoint,
    devicePosition: GeoPoint?,
    deviceAccuracyMeters: Float?,
    deviceAzimuth: Float,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenInMaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distanceMeters = devicePosition?.distanceToMeters(target)
    val bearing = devicePosition?.initialBearingTo(target)

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = machine.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (machine.location.isNotBlank()) {
                    Text(
                        text = machine.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = String.format(
                        Locale.US,
                        "Target: %.6f, %.6f",
                        target.latitude,
                        target.longitude
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        CompassArrow(
            bearingToTarget = bearing,
            deviceAzimuth = deviceAzimuth,
            modifier = Modifier.size(220.dp)
        )

        Text(
            text = if (distanceMeters != null) {
                formatDistance(distanceMeters)
            } else {
                "Locating…"
            },
            style = MaterialTheme.typography.displaySmall
        )
        if (bearing != null) {
            Text(
                text = "${bearingToDirection(bearing)} (${bearing.toInt()}°)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Turn the device until the arrow points up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (deviceAccuracyMeters != null && deviceAccuracyMeters > 0f) {
            Text(
                text = "GPS accuracy: ±${deviceAccuracyMeters.toInt()} m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!hasPermission) {
            Button(onClick = onRequestPermission) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enable location")
            }
        }
        OutlinedButton(
            onClick = onOpenInMaps,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Explore, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Maps")
        }
    }
}

@Composable
private fun CompassArrow(
    bearingToTarget: Float?,
    deviceAzimuth: Float,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.92f

        drawCircle(color = outline, radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

        val ticks = 8
        for (i in 0 until ticks) {
            val angle = i * 360f / ticks
            rotate(degrees = angle, pivot = center) {
                drawLine(
                    color = onSurfaceVariant,
                    start = center.copy(y = center.y - radius),
                    end = center.copy(y = center.y - radius * 0.9f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        val relativeBearing = if (bearingToTarget != null) {
            ((bearingToTarget - deviceAzimuth) % 360f + 360f) % 360f
        } else {
            0f
        }

        rotate(degrees = relativeBearing, pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - radius * 0.78f)
                lineTo(center.x - radius * 0.16f, center.y - radius * 0.42f)
                lineTo(center.x, center.y - radius * 0.52f)
                lineTo(center.x + radius * 0.16f, center.y - radius * 0.42f)
                close()
            }
            drawPath(path = arrowPath, color = primary)
            drawCircle(
                color = primary,
                radius = radius * 0.06f,
                center = center.copy(y = center.y - radius * 0.42f)
            )
        }

        drawCircle(color = Color.White, radius = radius * 0.045f, center = center)
    }
}
