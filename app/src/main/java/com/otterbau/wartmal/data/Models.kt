package com.otterbau.wartmal.data

import java.util.UUID

enum class CheckStatus(val label: String, val sortOrder: Int) {
    PENDING("Pending", 1),
    PASSED("Passed", 2),
    FAILED("Failed", 0)
}

data class Check(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val status: CheckStatus = CheckStatus.PENDING,
    val performedAt: Long? = null,
    val note: String = ""
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

fun List<Check>.sortedForDisplay(): List<Check> =
    sortedWith(
        compareBy<Check> { it.status.sortOrder }
            .thenByDescending { it.performedAt ?: Long.MIN_VALUE }
    )

data class Machine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serialNumber: String,
    val location: String,
    val checks: List<Check> = emptyList()
)
