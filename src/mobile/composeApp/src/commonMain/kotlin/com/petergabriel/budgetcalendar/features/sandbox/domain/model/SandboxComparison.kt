package com.petergabriel.budgetcalendar.features.sandbox.domain.model

data class SandboxComparison(
    val realSafeToSpend: Long,
    val sandboxSafeToSpend: Long,
    val difference: Long,
    val addedTransactions: Int,
    val promotedCount: Int,
)
