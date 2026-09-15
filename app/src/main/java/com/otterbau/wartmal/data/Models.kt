package com.otterbau.wartmal.data

import java.util.UUID

enum class CheckStatus(val label: String) {
    PENDING("Pending"),
    PASSED("Passed"),
    FAILED("Failed")
}

data class Check(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val status: CheckStatus = CheckStatus.PENDING,
    val performedAt: Long = System.currentTimeMillis()
)

data class Machine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serialNumber: String,
    val location: String,
    val checks: List<Check> = emptyList()
)
