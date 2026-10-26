package com.otterbau.wartmal.data

private const val MAX_FIELD_LENGTH = 120
private const val MAX_DESCRIPTION_LENGTH = 1000
private const val MAX_NOTE_LENGTH = 1000

class MachineRepository {

    private fun requireValidText(value: String, field: String, maxLength: Int) {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "$field must not be blank" }
        require(trimmed.length <= maxLength) { "$field must be at most $maxLength characters" }
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
                    note = "Oil level at 3/4, topped up slightly."
                ),
                Check(
                    title = "Coolant concentration",
                    description = "Refractometer reading between 8-10%.",
                    status = CheckStatus.PENDING
                ),
                Check(
                    title = "Way covers intact",
                    description = "Inspect for cracks or debris.",
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
                    note = "No leaks found."
                ),
                Check(
                    title = "Emergency stop test",
                    description = "Verify e-stop halts ram within 1 second.",
                    status = CheckStatus.PENDING
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
                    status = CheckStatus.PENDING
                )
            )
        )
    )

    fun getMachines(): List<Machine> = machines.toList()

    fun getMachine(id: String): Machine? = machines.find { it.id == id }

    fun addMachine(name: String, serialNumber: String, location: String): Machine {
        requireValidText(name, "Machine name", MAX_FIELD_LENGTH)
        require(serialNumber.isBlank() || serialNumber.length <= MAX_FIELD_LENGTH) {
            "Serial number must be at most $MAX_FIELD_LENGTH characters"
        }
        require(location.isBlank() || location.length <= MAX_FIELD_LENGTH) {
            "Location must be at most $MAX_FIELD_LENGTH characters"
        }
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

    fun addCheck(machineId: String, title: String, description: String): Check {
        require(machines.any { it.id == machineId }) { "Unknown machine id: $machineId" }
        requireValidText(title, "Check title", MAX_FIELD_LENGTH)
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "Check description must be at most $MAX_DESCRIPTION_LENGTH characters"
        }
        val check = Check(title = title.trim(), description = description.trim())
        val index = machines.indexOfFirst { it.id == machineId }
        machines[index] = machines[index].copy(checks = machines[index].checks + check)
        return check
    }

    fun updateCheck(machineId: String, checkId: String, title: String, description: String): Boolean {
        requireValidText(title, "Check title", MAX_FIELD_LENGTH)
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "Check description must be at most $MAX_DESCRIPTION_LENGTH characters"
        }
        val index = machines.indexOfFirst { it.id == machineId }
        if (index < 0) return false
        val machine = machines[index]
        val checkIndex = machine.checks.indexOfFirst { it.id == checkId }
        if (checkIndex < 0) return false
        val updated = machine.checks[checkIndex].copy(
            title = title.trim(),
            description = description.trim()
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
        require(note.length <= MAX_NOTE_LENGTH) {
            "Check note must be at most $MAX_NOTE_LENGTH characters"
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
                current.copy(
                    status = status,
                    performedAt = performedAt,
                    note = note.trim()
                )
            }
        }
        val checks = machine.checks.toMutableList().apply { set(checkIndex, updated) }
        machines[index] = machine.copy(checks = checks)
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
