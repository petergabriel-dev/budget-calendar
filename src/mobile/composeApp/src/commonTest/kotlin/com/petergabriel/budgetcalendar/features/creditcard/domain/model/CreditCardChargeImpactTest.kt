package com.petergabriel.budgetcalendar.features.creditcard.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreditCardChargeImpactTest {
    @Test
    fun exceedsCredit_respectsBoundaryAndNullLimit() {
        assertFalse(calculateCreditCardChargeImpact(0L, 0L, 1_000L, 999L).exceedsCredit)
        assertFalse(calculateCreditCardChargeImpact(0L, 0L, 1_000L, 1_000L).exceedsCredit)
        assertTrue(calculateCreditCardChargeImpact(0L, 0L, 1_000L, 1_001L).exceedsCredit)
        assertFalse(calculateCreditCardChargeImpact(0L, 0L, null, 100_000L).exceedsCredit)
    }

    @Test
    fun reserveDeltaForInDebtCardEqualsNewChargeWhenExistingDebtReserved() {
        val impact = calculateCreditCardChargeImpact(
            currentBalance = -5_000L,
            reservedAmount = 5_000L,
            availableCredit = 20_000L,
            amount = 1_200L,
        )

        assertEquals(6_200L, impact.owedAfter)
        assertEquals(1_200L, impact.reserveDelta)
    }

    @Test
    fun reserveDeltaForOverpaidCardCanBeLessThanCharge() {
        val impact = calculateCreditCardChargeImpact(
            currentBalance = 500L,
            reservedAmount = 0L,
            availableCredit = 20_000L,
            amount = 1_200L,
        )

        assertEquals(700L, impact.owedAfter)
        assertEquals(700L, impact.reserveDelta)
    }
}
