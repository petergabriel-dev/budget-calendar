package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.ConsequencesResult
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SimulationInput
import kotlin.math.max

class RunSimulationUseCase {
    operator fun invoke(
        simulationInput: SimulationInput,
        activeSnapshot: SandboxSnapshot,
        currentDailyRate: Long,
        daysRemainingInMonth: Int,
    ): ConsequencesResult {
        val safeDaysRemaining = daysRemainingInMonth.coerceAtLeast(1)
        val newSafeToSpend = activeSnapshot.initialSafeToSpend - simulationInput.amount
        val projectedDailyRate = newSafeToSpend / safeDaysRemaining
        val dailyVelocityImpact = projectedDailyRate - currentDailyRate
        val daysOfRunway = when {
            currentDailyRate <= 0L -> 0
            newSafeToSpend <= 0L -> 0
            else -> max(0L, newSafeToSpend / currentDailyRate).toInt()
        }

        return ConsequencesResult(
            newSafeToSpend = newSafeToSpend,
            dailyVelocityImpact = dailyVelocityImpact,
            daysOfRunway = daysOfRunway,
            isAffordable = newSafeToSpend >= 0L,
        )
    }
}
