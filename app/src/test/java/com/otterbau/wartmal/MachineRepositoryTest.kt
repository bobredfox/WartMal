package com.otterbau.wartmal

import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.CheckSummary
import com.otterbau.wartmal.data.GeoPoint
import com.otterbau.wartmal.data.MachineRepository
import com.otterbau.wartmal.data.RecurrenceInterval
import com.otterbau.wartmal.data.dueSoonCount
import com.otterbau.wartmal.data.overdueCount
import com.otterbau.wartmal.data.sortedForDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class MachineRepositoryTest {

    private lateinit var repository: MachineRepository

    @Before
    fun setUp() {
        repository = MachineRepository()
    }

    private fun newMachine(): String =
        repository.addMachine("Test Machine", "SN-1", "Hall 1").id

    @Test
    fun addMachine_trimsFields() {
        val machine = repository.addMachine("  Press  ", "  SN-9  ", "  Hall 2 ")
        assertEquals("Press", machine.name)
        assertEquals("SN-9", machine.serialNumber)
        assertEquals("Hall 2", machine.location)
    }

    @Test
    fun addMachine_storesOptionalLocationPoint() {
        val point = GeoPoint(latitude = 52.52, longitude = 13.405)
        val machine = repository.addMachine("Press", "SN-1", "Hall 1", locationPoint = point)
        assertEquals(point, machine.locationPoint)
        assertEquals(point, repository.getMachine(machine.id)!!.locationPoint)
    }

    @Test
    fun addMachine_withoutLocationPoint_defaultsToNull() {
        val machine = repository.addMachine("Press", "SN-1", "Hall 1")
        assertNull(machine.locationPoint)
    }

    @Test
    fun updateMachine_canSetChangeAndClearLocationPoint() {
        val machine = repository.addMachine("Press", "SN-1", "Hall 1")
        val point = GeoPoint(latitude = 48.1, longitude = 11.6)
        repository.updateMachine(machine.copy(locationPoint = point))
        assertEquals(point, repository.getMachine(machine.id)!!.locationPoint)

        repository.updateMachine(machine.copy(locationPoint = null))
        assertNull(repository.getMachine(machine.id)!!.locationPoint)
    }

    @Test
    fun seedData_firstMachineHasCoordinates() {
        val machine = repository.getMachines().first()
        assertNotNull(machine.locationPoint)
    }

    @Test
    fun addMachine_rejectsBlankName() {
        try {
            repository.addMachine("   ", "SN", "Hall")
            fail("Expected IllegalArgumentException for blank name")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun addMachine_rejectsTooLongName() {
        try {
            repository.addMachine("x".repeat(121), "SN", "Hall")
            fail("Expected IllegalArgumentException for name over 120 chars")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun addCheck_trimsAndDefaultsToPending() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "  Oil level ", "  Top up ")
        assertEquals("Oil level", check.title)
        assertEquals("Top up", check.description)
        assertEquals(CheckStatus.PENDING, check.status)
        assertNull(check.performedAt)
        assertEquals("", check.note)
    }

    @Test
    fun addCheck_rejectsBlankTitle() {
        val machineId = newMachine()
        try {
            repository.addCheck(machineId, "  ", "desc")
            fail("Expected IllegalArgumentException for blank title")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun addCheck_rejectsUnknownMachine() {
        try {
            repository.addCheck("does-not-exist", "Title", "desc")
            fail("Expected IllegalArgumentException for unknown machine")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun updateCheck_replacesTitleAndDescription() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Old", "Old desc")
        assertTrue(repository.updateCheck(machineId, check.id, "New", "New desc"))
        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals("New", updated.title)
        assertEquals("New desc", updated.description)
        assertEquals(check.id, updated.id)
    }

    @Test
    fun updateCheck_returnsFalseForUnknownIds() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        assertFalse(repository.updateCheck(machineId, "unknown-check", "x", "y"))
        assertFalse(repository.updateCheck("unknown-machine", check.id, "x", "y"))
    }

    @Test
    fun updateCheck_rejectsBlankTitle() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        try {
            repository.updateCheck(machineId, check.id, " ", "desc")
            fail("Expected IllegalArgumentException for blank title")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun updateCheckStatus_passedSetsTimestampAndNote() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        assertTrue(repository.updateCheckStatus(machineId, check.id, CheckStatus.PASSED, "ok", now = 42L))
        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals(CheckStatus.PASSED, updated.status)
        assertEquals(42L, updated.performedAt)
        assertEquals("ok", updated.note)
    }

    @Test
    fun updateCheckStatus_pendingResetsTimestampAndNote() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        repository.updateCheckStatus(machineId, check.id, CheckStatus.FAILED, "bad", now = 42L)
        assertTrue(repository.updateCheckStatus(machineId, check.id, CheckStatus.PENDING, now = 99L))
        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals(CheckStatus.PENDING, updated.status)
        assertNull(updated.performedAt)
        assertEquals("", updated.note)
    }

    @Test
    fun updateCheckStatus_repeatedCompletionKeepsOriginalTimestamp() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        repository.updateCheckStatus(machineId, check.id, CheckStatus.FAILED, "bad", now = 42L)
        repository.updateCheckStatus(machineId, check.id, CheckStatus.FAILED, "worse", now = 100L)
        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals(42L, updated.performedAt)
        assertEquals("worse", updated.note)
    }

    @Test
    fun updateCheckStatus_switchingStatusUpdatesTimestamp() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        repository.updateCheckStatus(machineId, check.id, CheckStatus.FAILED, "bad", now = 42L)
        repository.updateCheckStatus(machineId, check.id, CheckStatus.PASSED, "fixed", now = 100L)
        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals(CheckStatus.PASSED, updated.status)
        assertEquals(100L, updated.performedAt)
    }

    @Test
    fun updateCheckStatus_returnsFalseForUnknownIds() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        assertFalse(repository.updateCheckStatus(machineId, "unknown-check", CheckStatus.PASSED))
        assertFalse(repository.updateCheckStatus("unknown-machine", check.id, CheckStatus.PASSED))
    }

    @Test
    fun updateCheckStatus_rejectsTooLongNote() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Title", "desc")
        try {
            repository.updateCheckStatus(machineId, check.id, CheckStatus.PASSED, "n".repeat(1001))
            fail("Expected IllegalArgumentException for note over 1000 chars")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun deleteCheck_removesOnlyThatCheck() {
        val machineId = newMachine()
        val first = repository.addCheck(machineId, "First", "")
        val second = repository.addCheck(machineId, "Second", "")
        repository.deleteCheck(machineId, first.id)
        val remaining = repository.getMachine(machineId)!!.checks
        assertEquals(1, remaining.size)
        assertEquals(second.id, remaining.single().id)
    }

    @Test
    fun seedData_finishedChecksHaveTimestampAndPendingChecksDoNot() {
        val machine = repository.getMachines().first()
        val passed = machine.checks.single { it.status == CheckStatus.PASSED }
        val pending = machine.checks.single { it.status == CheckStatus.PENDING }
        assertNotNull(passed.performedAt)
        assertNull(pending.performedAt)
    }

    @Test
    fun checkSummary_countsStatuses() {
        val checks = listOf(
            repository.addCheck(newMachine(), "a", ""),
            run {
                val id = newMachine()
                repository.updateCheckStatus(
                    id,
                    repository.addCheck(id, "b", "").id,
                    CheckStatus.PASSED
                )
                repository.getMachine(id)!!.checks.single()
            },
            run {
                val id = newMachine()
                repository.updateCheckStatus(
                    id,
                    repository.addCheck(id, "c", "").id,
                    CheckStatus.FAILED
                )
                repository.getMachine(id)!!.checks.single()
            }
        )
        val summary = CheckSummary.from(checks)
        assertEquals(3, summary.total)
        assertEquals(1, summary.pending)
        assertEquals(1, summary.passed)
        assertEquals(1, summary.failed)
    }

    @Test
    fun sortedForDisplay_ordersFailedPendingPassedThenNewestFirst() {
        val machineId = newMachine()
        val passedOld = repository.addCheck(machineId, "passed old", "")
        repository.updateCheckStatus(machineId, passedOld.id, CheckStatus.PASSED, now = 10L)
        val passedNew = repository.addCheck(machineId, "passed new", "")
        repository.updateCheckStatus(machineId, passedNew.id, CheckStatus.PASSED, now = 20L)
        val failed = repository.addCheck(machineId, "failed", "")
        repository.updateCheckStatus(machineId, failed.id, CheckStatus.FAILED, now = 30L)
        val pending = repository.addCheck(machineId, "pending", "")
        val checks = repository.getMachine(machineId)!!.checks

        val sorted = checks.sortedForDisplay()

        assertEquals(listOf(failed.id, pending.id, passedNew.id, passedOld.id), sorted.map { it.id })
    }

    @Test
    fun addCheck_withRecurrenceSetsNextDueDate() {
        val machineId = newMachine()
        val check = repository.addCheck(
            machineId,
            "Oil change",
            "desc",
            RecurrenceInterval.WEEKLY,
            now = 1_000L
        )
        assertEquals(RecurrenceInterval.WEEKLY, check.recurrence)
        assertEquals(1_000L + 7 * 86_400_000L, check.nextDueAt)
    }

    @Test
    fun addCheck_withoutRecurrenceHasNoNextDueDate() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "One-off", "desc", now = 1_000L)
        assertEquals(RecurrenceInterval.NONE, check.recurrence)
        assertNull(check.nextDueAt)
    }

    @Test
    fun completeRecurringCheck_schedulesNextDueAndRecordsHistory() {
        val machineId = newMachine()
        val check = repository.addCheck(
            machineId,
            "Lubrication",
            "desc",
            RecurrenceInterval.DAILY,
            now = 1_000L
        )
        assertTrue(
            repository.updateCheckStatus(
                machineId,
                check.id,
                CheckStatus.PASSED,
                "done",
                now = 5_000L
            )
        )

        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals(CheckStatus.PENDING, updated.status)
        assertEquals(5_000L + 86_400_000L, updated.nextDueAt)
        assertNull(updated.performedAt)

        val history = repository.getHistory(machineId)
        assertEquals(1, history.size)
        assertEquals(check.id, history.single().checkId)
        assertEquals("Lubrication", history.single().checkTitle)
        assertEquals(CheckStatus.PASSED, history.single().status)
        assertEquals(5_000L, history.single().performedAt)
        assertEquals("done", history.single().note)
        assertEquals(RecurrenceInterval.DAILY, history.single().recurrence)
    }

    @Test
    fun completeNonRecurringCheck_keepsStatusAndDoesNotScheduleNextDue() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "One-off", "desc", now = 1_000L)
        repository.updateCheckStatus(
            machineId,
            check.id,
            CheckStatus.PASSED,
            "ok",
            now = 5_000L
        )

        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals(CheckStatus.PASSED, updated.status)
        assertEquals(5_000L, updated.performedAt)
        assertNull(updated.nextDueAt)

        val history = repository.getHistory(machineId)
        assertEquals(1, history.size)
        assertEquals(CheckStatus.PASSED, history.single().status)
    }

    @Test
    fun history_accumulatesPerMachineAndIsSortedNewestFirst() {
        val machineId = newMachine()
        val check = repository.addCheck(
            machineId,
            "Recurring",
            "",
            RecurrenceInterval.WEEKLY,
            now = 0L
        )
        repository.updateCheckStatus(machineId, check.id, CheckStatus.PASSED, "first", now = 10L)
        repository.updateCheckStatus(machineId, check.id, CheckStatus.FAILED, "second", now = 20L)
        repository.updateCheckStatus(machineId, check.id, CheckStatus.PASSED, "third", now = 30L)

        val history = repository.getHistory(machineId)
        assertEquals(3, history.size)
        assertEquals(listOf(30L, 20L, 10L), history.map { it.performedAt })
        assertEquals(
            listOf("third", "second", "first"),
            history.map { it.note }
        )

        val otherMachineId = newMachine()
        assertEquals(0, repository.getHistory(otherMachineId).size)
    }

    @Test
    fun resetToPending_doesNotRecordHistory() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Check", "")
        repository.updateCheckStatus(machineId, check.id, CheckStatus.PASSED, "ok", now = 10L)
        repository.updateCheckStatus(machineId, check.id, CheckStatus.PENDING, now = 20L)
        assertEquals(1, repository.getHistory(machineId).size)
    }

    @Test
    fun overdueAndDueSoon_areDerivedFromNextDueDate() {
        val machineId = newMachine()
        val overdueCheck = repository.addCheck(
            machineId,
            "Overdue",
            "",
            RecurrenceInterval.DAILY,
            now = 0L
        )
        repository.addCheck(
            machineId,
            "Due soon",
            "",
            RecurrenceInterval.DAILY,
            now = 3 * 86_400_000L
        )
        repository.addCheck(machineId, "No recurrence", "")

        val now = 2 * 86_400_000L
        val machine = repository.getMachine(machineId)!!
        assertTrue(machine.checks.single { it.id == overdueCheck.id }.isOverdue(now))
        assertEquals(1, machine.overdueCount(now))
        assertEquals(1, machine.dueSoonCount(now))
    }

    @Test
    fun sortedForDisplay_putsOverdueChecksFirst() {
        val machineId = newMachine()
        val overdueCheck = repository.addCheck(
            machineId,
            "Overdue",
            "",
            RecurrenceInterval.DAILY,
            now = 0L
        )
        val normal = repository.addCheck(machineId, "Normal", "")
        val now = 5 * 86_400_000L
        val sorted = repository.getMachine(machineId)!!.checks.sortedForDisplay(now)
        assertEquals(overdueCheck.id, sorted.first().id)
        assertEquals(normal.id, sorted.last().id)
    }

    @Test
    fun updateCheck_changingRecurrenceUpdatesNextDueDate() {
        val machineId = newMachine()
        val check = repository.addCheck(machineId, "Check", "", now = 1_000L)
        assertTrue(
            repository.updateCheck(
                machineId,
                check.id,
                "Check",
                "",
                RecurrenceInterval.YEARLY,
                now = 2_000L
            )
        )
        val updated = repository.getMachine(machineId)!!.checks.single()
        assertEquals(RecurrenceInterval.YEARLY, updated.recurrence)
        assertEquals(2_000L + 365 * 86_400_000L, updated.nextDueAt)
    }

    @Test
    fun updateCheck_removingRecurrenceClearsNextDueDate() {
        val machineId = newMachine()
        val check = repository.addCheck(
            machineId,
            "Check",
            "",
            RecurrenceInterval.WEEKLY,
            now = 1_000L
        )
        assertTrue(
            repository.updateCheck(
                machineId,
                check.id,
                "Check",
                "",
                RecurrenceInterval.NONE,
                now = 2_000L
            )
        )
        val updated = repository.getMachine(machineId)!!.checks.single()
        assertNull(updated.nextDueAt)
    }
}
