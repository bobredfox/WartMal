package com.otterbau.wartmal

import com.otterbau.wartmal.data.CheckStatus
import com.otterbau.wartmal.data.CheckSummary
import com.otterbau.wartmal.data.MachineRepository
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
}
