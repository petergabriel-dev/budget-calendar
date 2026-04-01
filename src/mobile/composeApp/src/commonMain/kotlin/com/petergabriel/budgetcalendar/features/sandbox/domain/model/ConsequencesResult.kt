package com.petergabriel.budgetcalendar.features.sandbox.domain.model

data class ConsequencesResult(
    val newSafeToSpend: Long,
    val dailyVelocityImpact: Long,
    val daysOfRunway: Int,
    val isAffordable: Boolean,
)
