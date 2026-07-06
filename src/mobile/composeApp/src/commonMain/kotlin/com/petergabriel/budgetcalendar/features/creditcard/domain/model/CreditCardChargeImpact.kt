package com.petergabriel.budgetcalendar.features.creditcard.domain.model

import kotlin.math.max

data class CreditCardChargeImpact(
    val exceedsCredit: Boolean,
    val reserveDelta: Long,
    val owedAfter: Long,
)

fun calculateCreditCardChargeImpact(
    currentBalance: Long,
    reservedAmount: Long,
    availableCredit: Long?,
    amount: Long,
): CreditCardChargeImpact {
    val owedAfter = amount - currentBalance
    val reserveDelta = (max(0L, owedAfter) - reservedAmount)
        .coerceIn(0L, amount)

    return CreditCardChargeImpact(
        exceedsCredit = availableCredit != null && amount > availableCredit,
        reserveDelta = reserveDelta,
        owedAfter = owedAfter,
    )
}
