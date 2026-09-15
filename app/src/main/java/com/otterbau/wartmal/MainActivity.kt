package com.otterbau.wartmal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otterbau.wartmal.ui.AddEditMachineScreen
import com.otterbau.wartmal.ui.MachineDetailScreen
import com.otterbau.wartmal.ui.MachineListScreen
import com.otterbau.wartmal.ui.MachineViewModel
import com.otterbau.wartmal.ui.Screen
import com.otterbau.wartmal.ui.theme.WartMalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WartMalTheme {
                val viewModel: MachineViewModel = viewModel()
                val screen by viewModel.screen.collectAsState()

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