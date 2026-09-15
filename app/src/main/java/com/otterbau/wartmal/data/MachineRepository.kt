package com.otterbau.wartmal.data

class MachineRepository {

    private val machines = mutableListOf(
        Machine(
            name = "CNC Milling Machine A1",
            serialNumber = "CNC-A1-2023-0042",
            location = "Workshop Floor 1 - Bay 3",
            checks = listOf(
                Check(
                    title = "Spindle oil level",
                    description = "Check oil level and top up if below min mark.",
                    status = CheckStatus.PASSED
                ),
                Check(
                    title = "Coolant concentration",
                    description = "Refractometer reading between 8-10%.",
                    status = CheckStatus.PENDING
                ),
                Check(
                    title = "Way covers intact",
                    description = "Inspect for cracks or debris.",
                    status = CheckStatus.FAILED
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
                    status = CheckStatus.PASSED
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
        val machine = Machine(name = name, serialNumber = serialNumber, location = location)
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
        val check = Check(title = title, description = description)
        val index = machines.indexOfFirst { it.id == machineId }
        if (index >= 0) {
            machines[index] = machines[index].copy(checks = machines[index].checks + check)
        }
        return check
    }

    fun updateCheckStatus(machineId: String, checkId: String, status: CheckStatus) {
        val index = machines.indexOfFirst { it.id == machineId }
        if (index >= 0) {
            val updatedChecks = machines[index].checks.map { check ->
                if (check.id == checkId) check.copy(status = status) else check
            }
            machines[index] = machines[index].copy(checks = updatedChecks)
        }
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
