package com.otterbau.wartmal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otterbau.wartmal.ui.AddEditMachineScreen
import com.otterbau.wartmal.ui.FindMachineScreen
import com.otterbau.wartmal.ui.MachineDetailScreen
import com.otterbau.wartmal.ui.MachineListScreen
import com.otterbau.wartmal.ui.MachineViewModel
import com.otterbau.wartmal.ui.MyDayScreen
import com.otterbau.wartmal.ui.RootTab
import com.otterbau.wartmal.ui.Screen
import com.otterbau.wartmal.ui.UiMessage
import com.otterbau.wartmal.ui.theme.WartMalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WartMalTheme {
                val viewModel: MachineViewModel = viewModel()
                val screen by viewModel.screen.collectAsState()
                val selectedTab by viewModel.selectedTab.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var lastMessageWasError by remember { mutableStateOf(false) }

                LaunchedEffect(viewModel) {
                    viewModel.uiMessages.collect { message ->
                        lastMessageWasError = message is UiMessage.Error
                        snackbarHostState.showSnackbar(message.text)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = if (lastMessageWasError) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.inverseSurface
                                },
                                contentColor = if (lastMessageWasError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.inverseOnSurface
                                },
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    },
                    bottomBar = {
                        if (screen == null) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == RootTab.MyDay,
                                    onClick = { viewModel.navigateToTab(RootTab.MyDay) },
                                    icon = { Icon(Icons.Filled.Today, contentDescription = null) },
                                    label = { Text("My Day") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == RootTab.Machines,
                                    onClick = { viewModel.navigateToTab(RootTab.Machines) },
                                    icon = { Icon(Icons.Filled.Build, contentDescription = null) },
                                    label = { Text("Machines") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val current = screen) {
                            null -> when (selectedTab) {
                                RootTab.MyDay -> MyDayScreen(viewModel)
                                RootTab.Machines -> MachineListScreen(viewModel)
                            }

                            is Screen.MachineDetail -> MachineDetailScreen(
                                viewModel = viewModel,
                                machineId = current.machineId
                            )

                            is Screen.AddMachine -> AddEditMachineScreen(
                                viewModel = viewModel,
                                machineId = null,
                                isEdit = false
                            )

                            is Screen.EditMachine -> AddEditMachineScreen(
                                viewModel = viewModel,
                                machineId = current.machineId,
                                isEdit = true
                            )

                            is Screen.FindMachine -> FindMachineScreen(
                                viewModel = viewModel,
                                machineId = current.machineId
                            )
                        }
                    }
                }
            }
        }
    }
}
