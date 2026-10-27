package com.otterbau.wartmal.data

private const val MAX_FIELD_LENGTH = 120
private const val MAX_DESCRIPTION_LENGTH = 1000
private const val MAX_NOTE_LENGTH = 1000
private const val MAX_HISTORY_ENTRIES = 500

class MachineRepository {

    private fun requireValidText(value: String, field: String, maxLength: Int) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) throw WartMalException("$field must not be blank")
        if (trimmed.length > maxLength) {
            throw WartMalException("$field must be at most $maxLength characters")
        }
    }

    private fun requireValidOptionalText(value: String, field: String, maxLength: Int) {
        if (value.isNotBlank() && value.trim().length > maxLength) {
            throw WartMalException("$field must be at most $maxLength characters")
        }
    }

    private val machines = mutableListOf(
        Machine(
            name = "CNC Milling Machine A1",
            serialNumber = "CNC-A1-2023-0042",
            location = "Workshop Floor 1 - Bay 3",
            checks = listOf(
                Check(
                    title = "Spindle oil level",
                    description = "Check oil level and top up if below min mark.",
                    status = CheckStatus.PASSED,
                    performedAt = System.currentTimeMillis() - 86_400_000L,
                    note = "Oil level at 3/4, topped up slightly.",
                    recurrence = RecurrenceInterval.WEEKLY,
                    nextDueAt = System.currentTimeMillis() - 86_400_000L
                ),
                Check(
                    title = "Coolant concentration",
                    description = "Refractometer reading between 8-10%.",
                    status = CheckStatus.PENDING,
                    recurrence = RecurrenceInterval.MONTHLY,
                    nextDueAt = System.currentTimeMillis() + 5 * 86_400_000L
                ),
                Check(
                    title = "Way covers intact",
                    description = "Inspect for cracks or debris.",
                    status = CheckStatus.FAILED,
                    performedAt = System.currentTimeMillis() - 172_800_000L,
                    note = "Crack on left cover, replacement ordered."
                )
            ),
            history = listOf(
                MaintenanceHistoryEntry(
                    checkId = "seed-spindle",
                    checkTitle = "Spindle oil level",
                    status = CheckStatus.PASSED,
                    performedAt = System.currentTimeMillis() - 86_400_000L,
                    note = "Oil level at 3/4, topped up slightly.",
                    recurrence = RecurrenceInterval.WEEKLY
                ),
                MaintenanceHistoryEntry(
                    checkId = "seed-waycovers",
                    checkTitle = "Way covers intact",
                    status = CheckStatus.FAILED,
                    performedAt = System.currentTimeMillis() - 172_800_000L,
                    note = "Crack on left cover, replacement ordered."
                )
            )
        ),
        Machine(
            name = "Hydraulic Press HP-200",
            serialNumber = "HP200-2019-0118",
            location = "Workshop Floor 2 - Bay 1",
            checks = listOf(
                Check(
                    title = "Hydraulic fluid leak",
                    description = "Visual inspection of all seals and hoses.",
                    status = CheckStatus.PASSED,
                    performedAt = System.currentTimeMillis() - 43_200_000L,
                    note = "No leaks found.",
                    recurrence = RecurrenceInterval.DAILY,
                    nextDueAt = System.currentTimeMillis() - 43_200_000L
                ),
                Check(
                    title = "Emergency stop test",
                    description = "Verify e-stop halts ram within 1 second.",
                    status = CheckStatus.PENDING,
                    recurrence = RecurrenceInterval.WEEKLY,
                    nextDueAt = System.currentTimeMillis() + 2 * 86_400_000L
                )
            ),
            history = listOf(
                MaintenanceHistoryEntry(
                    checkId = "seed-leak",
                    checkTitle = "Hydraulic fluid leak",
                    status = CheckStatus.PASSED,
                    performedAt = System.currentTimeMillis() - 43_200_000L,
                    note = "No leaks found.",
                    recurrence = RecurrenceInterval.DAILY
                )
            )
        ),
        Machine(
            name = "Conveyor Belt CB-7",
            serialNumber = "CB7-2021-0503",
            location = "Packaging Line - Section 4",
            checks = listOf(
                Check(
                    title = "Belt tension",
                    description = "Measure deflection; adjust to spec.",
                    status = CheckStatus.PENDING,
                    recurrence = RecurrenceInterval.MONTHLY,
                    nextDueAt = System.currentTimeMillis() + 10 * 86_400_000L
                )
            )
        )
    )

    fun getMachines(): List<Machine> = machines.toList()

    fun getMachine(id: String): Machine? = machines.find { it.id == id }

    fun getHistory(machineId: String): List<MaintenanceHistoryEntry> =
        getMachine(machineId)?.history?.sortedByDescending { it.performedAt } ?: emptyList()

    fun addMachine(name: String, serialNumber: String, location: String): Machine {
        requireValidText(name, "Machine name", MAX_FIELD_LENGTH)
        requireValidOptionalText(serialNumber, "Serial number", MAX_FIELD_LENGTH)
        requireValidOptionalText(location, "Location", MAX_FIELD_LENGTH)
        val machine = Machine(
            name = name.trim(),
            serialNumber = serialNumber.trim(),
            location = location.trim()
        )
        machines.add(machine)
        return machine
    }

    fun updateMachine(machine: Machine) {
        val index = machines.indexOfFirst { it.id == machine.id }
        if (index >= 0) machines[index] = machine
    }

    fun deleteMachine(id: String) {
        machines.removeAll { it.id == id }
    }

    fun addCheck(
        machineId: String,
        title: String,
        description: String,
        recurrence: RecurrenceInterval = RecurrenceInterval.NONE,
        now: Long = System.currentTimeMillis()
    ): Check {
        val index = machines.indexOfFirst { it.id == machineId }
        if (index < 0) throw WartMalException("Machine not found")
        requireValidText(title, "Check title", MAX_FIELD_LENGTH)
        if (description.length > MAX_DESCRIPTION_LENGTH) {
            throw WartMalException("Check description must be at most $MAX_DESCRIPTION_LENGTH characters")
        }
        val check = Check(
            title = title.trim(),
            description = description.trim(),
            recurrence = recurrence,
            nextDueAt = recurrence.nextDue(now)
        )
        machines[index] = machines[index].copy(checks = machines[index].checks + check)
        return check
    }

    fun updateCheck(
        machineId: String,
        checkId: String,
        title: String,
        description: String,
        recurrence: RecurrenceInterval = RecurrenceInterval.NONE,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        requireValidText(title, "Check title", MAX_FIELD_LENGTH)
        if (description.length > MAX_DESCRIPTION_LENGTH) {
            throw WartMalException("Check description must be at most $MAX_DESCRIPTION_LENGTH characters")
        }
        val index = machines.indexOfFirst { it.id == machineId }
        if (index < 0) return false
        val machine = machines[index]
        val checkIndex = machine.checks.indexOfFirst { it.id == checkId }
        if (checkIndex < 0) return false
        val current = machine.checks[checkIndex]
        val nextDueAt = when {
            recurrence == RecurrenceInterval.NONE -> null
            current.status == CheckStatus.PENDING && current.nextDueAt != null ->
                current.nextDueAt

            else -> recurrence.nextDue(current.performedAt ?: now)
        }
        val updated = current.copy(
            title = title.trim(),
            description = description.trim(),
            recurrence = recurrence,
            nextDueAt = nextDueAt
        )
        val checks = machine.checks.toMutableList().apply { set(checkIndex, updated) }
        machines[index] = machine.copy(checks = checks)
        return true
    }

    fun updateCheckStatus(
        machineId: String,
        checkId: String,
        status: CheckStatus,
        note: String = "",
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (note.length > MAX_NOTE_LENGTH) {
            throw WartMalException("Check note must be at most $MAX_NOTE_LENGTH characters")
        }
        val index = machines.indexOfFirst { it.id == machineId }
        if (index < 0) return false
        val machine = machines[index]
        val checkIndex = machine.checks.indexOfFirst { it.id == checkId }
        if (checkIndex < 0) return false
        val current = machine.checks[checkIndex]
        val updated = when (status) {
            CheckStatus.PENDING -> current.copy(
                status = status,
                performedAt = null,
                note = ""
            )

            CheckStatus.PASSED, CheckStatus.FAILED -> {
                val performedAt = if (current.status == status && current.performedAt != null) {
                    current.performedAt
                } else {
                    now
                }
                if (current.recurrence != RecurrenceInterval.NONE) {
                    current.copy(
                        status = CheckStatus.PENDING,
                        performedAt = null,
                        note = "",
                        nextDueAt = current.recurrence.nextDue(performedAt)
                    )
                } else {
                    current.copy(
                        status = status,
                        performedAt = performedAt,
                        note = note.trim()
                    )
                }
            }
        }
        val historyEntry = if (status == CheckStatus.PENDING) {
            null
        } else {
            MaintenanceHistoryEntry(
                checkId = current.id,
                checkTitle = current.title,
                status = status,
                performedAt = updated.performedAt ?: now,
                note = if (current.recurrence != RecurrenceInterval.NONE) note.trim() else updated.note,
                recurrence = current.recurrence
            )
        }
        val history = if (historyEntry != null) {
            (machine.history + historyEntry).takeLast(MAX_HISTORY_ENTRIES)
        } else {
            machine.history
        }
        val checks = machine.checks.toMutableList().apply { set(checkIndex, updated) }
        machines[index] = machine.copy(checks = checks, history = history)
        return true
    }

    fun deleteCheck(machineId: String, checkId: String) {
        val index = machines.indexOfFirst { it.id == machineId }
        if (index >= 0) {
            machines[index] = machines[index].copy(
                checks = machines[index].checks.filterNot { it.id == checkId }
            )
        }
    }
}
