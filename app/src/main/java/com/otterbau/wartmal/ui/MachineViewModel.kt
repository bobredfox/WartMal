package com.otterbau.wartmal.ui

import androidx.lifecycle.ViewModel
import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.Machine
import com.otterbau.wartmal.data.MachineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Screen {
    data object MachineList : Screen()
    data class MachineDetail(val machineId: String) : Screen()
    data object AddMachine : Screen()
    data class EditMachine(val machineId: String) : Screen()
}

class MachineViewModel : ViewModel() {

    private val repository = MachineRepository()

    private val _machines = MutableStateFlow(repository.getMachines())
    val machines: StateFlow<List<Machine>> = _machines.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(Screen.MachineList)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun navigateToDetail(machineId: String) {
        _screen.value = Screen.MachineDetail(machineId)
    }

    fun navigateToAddMachine() {
        _screen.value = Screen.AddMachine
    }

    fun navigateToEditMachine(machineId: String) {
        _screen.value = Screen.EditMachine(machineId)
    }

    fun navigateBack() {
        _screen.value = Screen.MachineList
    }

    fun getMachine(id: String): Machine? = repository.getMachine(id)

    fun addMachine(name: String, serialNumber: String, location: String) {
        repository.addMachine(name, serialNumber, location)
        _machines.value = repository.getMachines()
        _screen.value = Screen.MachineList
    }

    fun updateMachine(machine: Machine, name: String, serialNumber: String, location: String) {
        repository.updateMachine(
            machine.copy(name = name, serialNumber = serialNumber, location = location)
        )
        _machines.value = repository.getMachines()
        _screen.value = Screen.MachineList
    }

    fun deleteMachine(id: String) {
        repository.deleteMachine(id)
        _machines.value = repository.getMachines()
        _screen.value = Screen.MachineList
    }

    fun addCheck(machineId: String, title: String, description: String) {
        repository.addCheck(machineId, title, description)
        _machines.value = repository.getMachines()
    }

    fun updateCheckStatus(machineId: String, checkId: String, status: CheckStatus) {
        repository.updateCheckStatus(machineId, checkId, status)
        _machines.value = repository.getMachines()
    }

    fun deleteCheck(machineId: String, checkId: String) {
        repository.deleteCheck(machineId, checkId)
        _machines.value = repository.getMachines()
    }
}
