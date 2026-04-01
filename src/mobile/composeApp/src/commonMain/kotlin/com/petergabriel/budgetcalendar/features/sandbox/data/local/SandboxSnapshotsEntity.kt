package com.petergabriel.budgetcalendar.features.sandbox.data.local

data class SandboxSnapshotsEntity(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val initialSafeToSpend: Long,
)

