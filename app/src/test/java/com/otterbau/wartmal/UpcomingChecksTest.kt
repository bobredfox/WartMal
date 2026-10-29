package com.otterbau.wartmal

import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.Machine
import com.otterbau.wartmal.data.MachineRepository
import com.otterbau.wartmal.data.RecurrenceInterval
import com.otterbau.wartmal.data.UpcomingCheck
import com.otterbau.wartmal.data.UpcomingGroup
import com.otterbau.wartmal.data.groupedByDay
import com.otterbau.wartmal.data.startOfDay
import com.otterbau.wartmal.data.upcomingChecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val DAY = 86_400_000L

class UpcomingChecksTest {

    private lateinit var repository: MachineRepository

    @Before
    fun setUp() {
        repository = MachineRepository()
    }

    private fun machineWithDueCheck(
        name: String,
        dueInDays: Long,
        now: Long,
        recurring: Boolean = true
    ): Pair<Machine, String> {
        val machine = repository.addMachine(name, "", "")
        val recurrence = if (recurring) RecurrenceInterval.DAILY else RecurrenceInterval.NONE
        val check = repository.addCheck(
            machine.id,
            "Check $name",
            "",
            recurrence,
            now = now + dueInDays * DAY - DAY
        )
        return repository.getMachine(machine.id)!! to check.id
    }

    @Test
    fun upcomingChecks_collectsPendingChecksAcrossMachines() {
        val now = System.currentTimeMillis()
        val (first, _) = machineWithDueCheck("A", dueInDays = 0, now = now)
        val (second, _) = machineWithDueCheck("B", dueInDays = 1, now = now)

        val upcoming = repository.getMachines().upcomingChecks(now = now)

        assertEquals(2, upcoming.size)
        assertTrue(upcoming.any { it.machineId == first.id })
        assertTrue(upcoming.any { it.machineId == second.id })
        assertEquals(first.name, upcoming.first { it.machineId == first.id }.machineName)
    }

    @Test
    fun upcomingChecks_excludesCompletedNonRecurringChecks() {
        val now = System.currentTimeMillis()
        val machineId = repository.addMachine("A", "", "").id
        val done = repository.addCheck(machineId, "Done", "", RecurrenceInterval.NONE, now = now)
        repository.updateCheckStatus(machineId, done.id, CheckStatus.PASSED, now = now)
        repository.addCheck(machineId, "Open", "", RecurrenceInterval.NONE, now = now)

        val upcoming = repository.getMachines().upcomingChecks(now = now)

        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun upcomingChecks_completedRecurringCheckReappearsWithNextDueDate() {
        val now = System.currentTimeMillis()
        val machineId = repository.addMachine("A", "", "").id
        val check = repository.addCheck(
            machineId,
            "Recurring",
            "",
            RecurrenceInterval.DAILY,
            now = now - 2 * DAY
        )
        repository.updateCheckStatus(machineId, check.id, CheckStatus.PASSED, now = now)

        val upcoming = repository.getMachines().upcomingChecks(now = now)

        assertEquals(1, upcoming.size)
        assertEquals(now + DAY, upcoming.single().check.nextDueAt)
    }

    @Test
    fun upcomingChecks_excludesChecksWithoutDueDateAndBeyondHorizon() {
        val now = System.currentTimeMillis()
        val machineId = repository.addMachine("A", "", "").id
        repository.addCheck(machineId, "No recurrence", "", RecurrenceInterval.NONE, now = now)
        repository.addCheck(
            machineId,
            "Far future",
            "",
            RecurrenceInterval.DAILY,
            now = now + 29 * DAY
        )

        val upcoming = repository.getMachines().upcomingChecks(now = now)

        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun upcomingChecks_sortsOverdueFirstThenByDueDate() {
        val now = System.currentTimeMillis()
        machineWithDueCheck("Overdue", dueInDays = -2, now = now)
        machineWithDueCheck("Later", dueInDays = 5, now = now)
        machineWithDueCheck("Soon", dueInDays = 1, now = now)

        val upcoming = repository.getMachines().upcomingChecks(now = now)

        assertEquals(listOf("Overdue", "Soon", "Later"), upcoming.map { it.machineName })
    }

    @Test
    fun groupedByDay_separatesOverdueTodayAndUpcoming() {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        val tomorrowStart = todayStart + DAY
        val machine = repository.addMachine("A", "", "")
        fun check(title: String, nextDueAt: Long) {
            repository.addCheck(machine.id, title, "", RecurrenceInterval.DAILY, now = nextDueAt - DAY)
        }
        check("Overdue", now - DAY)
        check("Today", (now + 3_600_000L).coerceAtMost(tomorrowStart - 1))
        check("Upcoming", todayStart + 3 * DAY)

        val grouped = repository.getMachines()
            .upcomingChecks(now = now, horizonMillis = 7 * DAY)
            .groupedByDay(now = now)

        assertEquals(listOf("Overdue"), grouped.getValue(UpcomingGroup.Overdue).map { it.check.title })
        assertEquals(listOf("Today"), grouped.getValue(UpcomingGroup.Today).map { it.check.title })
        assertEquals(listOf("Upcoming"), grouped.getValue(UpcomingGroup.Upcoming).map { it.check.title })
    }

    @Test
    fun groupedByDay_returnsEmptyListsForMissingGroups() {
        val grouped: Map<UpcomingGroup, List<UpcomingCheck>> = emptyList<UpcomingCheck>()
            .groupedByDay(now = System.currentTimeMillis())

        assertTrue(grouped.getValue(UpcomingGroup.Overdue).isEmpty())
        assertTrue(grouped.getValue(UpcomingGroup.Today).isEmpty())
        assertTrue(grouped.getValue(UpcomingGroup.Upcoming).isEmpty())
    }
}
