package com.otterbau.wartmal.ui

import androidx.lifecycle.ViewModel
import com.otterbau.wartmal.data.AppError
import com.otterbau.wartmal.data.AppResult
import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.Machine
import com.otterbau.wartmal.data.MachineRepository
import com.otterbau.wartmal.data.RecurrenceInterval
import com.otterbau.wartmal.data.WartMalException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Screen {
    data object MachineList : Screen()
    data class MachineDetail(val machineId: String) : Screen()
    data object AddMachine : Screen()
    data class EditMachine(val machineId: String) : Screen()
}

sealed class UiMessage {
    abstract val text: String
    data class Success(override val text: String) : UiMessage()
    data class Error(override val text: String) : UiMessage()
}

class MachineViewModel : ViewModel() {

    private val repository = MachineRepository()

    private val _machines = MutableStateFlow(repository.getMachines())
    val machines: StateFlow<List<Machine>> = _machines.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(Screen.MachineList)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _uiMessages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
    val uiMessages: SharedFlow<UiMessage> = _uiMessages.asSharedFlow()

    private fun <T> runCatching(block: () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (e: WartMalException) {
            AppResult.Failure(AppError.Validation(e.message ?: "Invalid input"))
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.Validation(e.message ?: "Invalid input"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message ?: "An unexpected error occurred"))
        }

    private fun <T> handle(
        successMessage: (T) -> String? = { null },
        onSuccess: (T) -> Unit
    ): (AppResult<T>) -> Unit = { result ->
        when (result) {
            is AppResult.Success -> {
                onSuccess(result.value)
                successMessage(result.value)?.let { emit(UiMessage.Success(it)) }
            }

            is AppResult.Failure -> emit(UiMessage.Error(result.error.message))
        }
    }

    private fun emit(message: UiMessage) {
        _uiMessages.tryEmit(message)
    }

    private fun refresh() {
        _machines.value = repository.getMachines()
    }

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
        runCatching { repository.addMachine(name, serialNumber, location) }
            .let(
                handle(successMessage = { "Machine \"${it.name}\" added" }) {
                    refresh()
                    _screen.value = Screen.MachineList
                }
            )
    }

    fun updateMachine(machine: Machine, name: String, serialNumber: String, location: String) {
        runCatching { repository.updateMachine(machine.copy(name = name, serialNumber = serialNumber, location = location)) }
            .let(
                handle(successMessage = { "Machine \"${name.trim()}\" saved" }) {
                    refresh()
                    _screen.value = Screen.MachineList
                }
            )
    }

    fun deleteMachine(id: String) {
        val name = repository.getMachine(id)?.name
        runCatching { repository.deleteMachine(id) }
            .let(
                handle(
                    successMessage = {
                        if (name != null) "Machine \"$name\" deleted" else "Machine deleted"
                    }
                ) {
                    refresh()
                    _screen.value = Screen.MachineList
                }
            )
    }

    fun addCheck(
        machineId: String,
        title: String,
        description: String,
        recurrence: RecurrenceInterval = RecurrenceInterval.NONE
    ) {
        runCatching { repository.addCheck(machineId, title, description, recurrence) }
            .let(
                handle(successMessage = { "Check \"${it.title}\" added" }) {
                    refresh()
                }
            )
    }

    fun updateCheck(
        machineId: String,
        checkId: String,
        title: String,
        description: String,
        recurrence: RecurrenceInterval = RecurrenceInterval.NONE
    ) {
        runCatching { repository.updateCheck(machineId, checkId, title, description, recurrence) }
            .let { result ->
                when (result) {
                    is AppResult.Success -> {
                        if (result.value) {
                            refresh()
                            emit(UiMessage.Success("Check \"${title.trim()}\" saved"))
                        } else {
                            emit(UiMessage.Error("Check could not be saved"))
                        }
                    }

                    is AppResult.Failure -> emit(UiMessage.Error(result.error.message))
                }
            }
    }

    fun updateCheckStatus(
        machineId: String,
        checkId: String,
        status: CheckStatus,
        note: String = ""
    ) {
        runCatching { repository.updateCheckStatus(machineId, checkId, status, note) }
            .let { result ->
                when (result) {
                    is AppResult.Success -> {
                        if (result.value) {
                            refresh()
                            val checkTitle = repository.getMachine(machineId)
                                ?.checks?.find { it.id == checkId }?.title
                            emit(
                                if (status == CheckStatus.PENDING) {
                                    UiMessage.Success("Check \"${checkTitle ?: ""}\" reset")
                                } else {
                                    UiMessage.Success(
                                        "Check \"${checkTitle ?: ""}\" marked as ${status.label.lowercase()}"
                                    )
                                }
                            )
                        } else {
                            emit(UiMessage.Error("Check could not be updated"))
                        }
                    }

                    is AppResult.Failure -> emit(UiMessage.Error(result.error.message))
                }
            }
    }

    fun deleteCheck(machineId: String, checkId: String) {
        val title = repository.getMachine(machineId)?.checks?.find { it.id == checkId }?.title
        runCatching { repository.deleteCheck(machineId, checkId) }
            .let(
                handle(
                    successMessage = {
                        if (title != null) "Check \"$title\" deleted" else "Check deleted"
                    }
                ) {
                    refresh()
                }
            )
    }
}
