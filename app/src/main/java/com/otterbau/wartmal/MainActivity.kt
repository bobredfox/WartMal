package com.otterbau.wartmal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otterbau.wartmal.ui.AddEditMachineScreen
import com.otterbau.wartmal.ui.MachineDetailScreen
import com.otterbau.wartmal.ui.MachineListScreen
import com.otterbau.wartmal.ui.MachineViewModel
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
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (screen) {
                            is Screen.MachineList -> MachineListScreen(viewModel)
                            is Screen.MachineDetail -> MachineDetailScreen(
                                viewModel = viewModel,
                                machineId = (screen as Screen.MachineDetail).machineId
                            )

                            is Screen.AddMachine -> AddEditMachineScreen(
                                viewModel = viewModel,
                                machineId = null,
                                isEdit = false
                            )

                            is Screen.EditMachine -> AddEditMachineScreen(
                                viewModel = viewModel,
                                machineId = (screen as Screen.EditMachine).machineId,
                                isEdit = true
                            )
                        }
                    }
                }
            }
        }
    }
}
