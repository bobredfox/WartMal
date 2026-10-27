package com.otterbau.wartmal.data

import java.util.UUID

private const val DAY_MILLIS = 86_400_000L
private const val DUE_SOON_THRESHOLD_MILLIS = 3 * DAY_MILLIS

enum class CheckStatus(val label: String, val sortOrder: Int) {
    PENDING("Pending", 1),
    PASSED("Passed", 2),
    FAILED("Failed", 0)
}

enum class RecurrenceInterval(val label: String, val days: Long) {
    NONE("No repeat", 0),
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
    YEARLY("Yearly", 365);

    fun nextDue(fromMillis: Long): Long? =
        if (this == NONE) null else fromMillis + days * DAY_MILLIS
}

data class Check(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val status: CheckStatus = CheckStatus.PENDING,
    val performedAt: Long? = null,
    val note: String = "",
    val recurrence: RecurrenceInterval = RecurrenceInterval.NONE,
    val nextDueAt: Long? = null
) {
    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean =
        status == CheckStatus.PENDING && nextDueAt != null && nextDueAt <= now

    fun isDueSoon(
        now: Long = System.currentTimeMillis(),
        thresholdMillis: Long = DUE_SOON_THRESHOLD_MILLIS
    ): Boolean =
        status == CheckStatus.PENDING &&
            nextDueAt != null &&
            nextDueAt > now &&
            nextDueAt <= now + thresholdMillis
}

data class MaintenanceHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val checkId: String,
    val checkTitle: String,
    val status: CheckStatus,
    val performedAt: Long,
    val note: String = "",
    val recurrence: RecurrenceInterval = RecurrenceInterval.NONE
)

data class CheckSummary(
    val total: Int,
    val pending: Int,
    val passed: Int,
    val failed: Int
) {
    companion object {
        fun from(checks: List<Check>): CheckSummary = CheckSummary(
            total = checks.size,
            pending = checks.count { it.status == CheckStatus.PENDING },
            passed = checks.count { it.status == CheckStatus.PASSED },
            failed = checks.count { it.status == CheckStatus.FAILED }
        )
    }
}

fun List<Check>.sortedForDisplay(now: Long = System.currentTimeMillis()): List<Check> =
    sortedWith(
        compareByDescending<Check> { it.isOverdue(now) }
            .thenBy { it.nextDueAt ?: Long.MAX_VALUE }
            .thenBy { it.status.sortOrder }
            .thenByDescending { it.performedAt ?: Long.MIN_VALUE }
    )

data class Machine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serialNumber: String,
    val location: String,
    val checks: List<Check> = emptyList(),
    val history: List<MaintenanceHistoryEntry> = emptyList()
)

fun Machine.overdueCount(now: Long = System.currentTimeMillis()): Int =
    checks.count { it.isOverdue(now) }

fun Machine.dueSoonCount(now: Long = System.currentTimeMillis()): Int =
    checks.count { it.isDueSoon(now) }
