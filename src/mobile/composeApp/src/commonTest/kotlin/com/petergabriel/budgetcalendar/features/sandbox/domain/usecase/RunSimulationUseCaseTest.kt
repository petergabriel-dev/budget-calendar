package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SimulationInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunSimulationUseCaseTest {
    private val useCase = RunSimulationUseCase()

    @Test
    fun affordableScenario_returnsAffordableWithPositiveRunway() {
        val result = useCase(
            simulationInput = SimulationInput(
                purchaseName = "Headphones",
                amount = 2_500L,
            ),
            activeSnapshot = snapshot(initialSafeToSpend = 10_000L),
            currentDailyRate = 300L,
            daysRemainingInMonth = 20,
        )

        assertEquals(7_500L, result.newSafeToSpend)
        assertEquals(75L, result.dailyVelocityImpact)
        assertTrue(result.daysOfRunway > 0)
        assertTrue(result.isAffordable)
    }

    @Test
    fun unaffordableScenario_returnsNegativeAndZeroRunway() {
        val result = useCase(
            simulationInput = SimulationInput(
                purchaseName = "Laptop",
                amount = 5_000L,
            ),
            activeSnapshot = snapshot(initialSafeToSpend = 2_000L),
            currentDailyRate = 400L,
            daysRemainingInMonth = 10,
        )

        assertTrue(result.newSafeToSpend < 0L)
        assertFalse(result.isAffordable)
        assertEquals(0, result.daysOfRunway)
    }

    @Test
    fun zeroDailyRate_doesNotThrowAndReturnsZeroRunway() {
        val result = useCase(
            simulationInput = SimulationInput(
                purchaseName = "Bills",
                amount = 1_000L,
            ),
            activeSnapshot = snapshot(initialSafeToSpend = 10_000L),
            currentDailyRate = 0L,
            daysRemainingInMonth = 15,
        )

        assertEquals(9_000L, result.newSafeToSpend)
        assertEquals(0, result.daysOfRunway)
        assertTrue(result.isAffordable)
    }

    @Test
    fun exactBoundary_amountEqualsInitialSafeToSpend_isAffordableWithZeroBalance() {
        val result = useCase(
            simulationInput = SimulationInput(
                purchaseName = "Exact Spend",
                amount = 5_000L,
            ),
            activeSnapshot = snapshot(initialSafeToSpend = 5_000L),
            currentDailyRate = 500L,
            daysRemainingInMonth = 10,
        )

        assertEquals(0L, result.newSafeToSpend)
        assertTrue(result.isAffordable)
        assertEquals(0, result.daysOfRunway)
    }

    private fun snapshot(initialSafeToSpend: Long): SandboxSnapshot {
        return SandboxSnapshot(
            id = 1L,
            name = "Scenario",
            description = null,
            createdAt = 0L,
            lastAccessedAt = 0L,
            initialSafeToSpend = initialSafeToSpend,
        )
    }
}
