package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransactionFormSheetLogicTest {
    private val tz = TimeZone.UTC

    @Test
    fun transactionFormEffectiveType_creditCardForcesExpense() {
        assertEquals(
            TransactionType.EXPENSE,
            transactionFormEffectiveType(TransactionType.INCOME, AccountType.CREDIT_CARD),
        )
        assertEquals(
            TransactionType.EXPENSE,
            transactionFormEffectiveType(TransactionType.TRANSFER, AccountType.CREDIT_CARD),
        )
    }

    @Test
    fun transactionFormEffectiveType_cashKeepsSelectedType() {
        assertEquals(
            TransactionType.TRANSFER,
            transactionFormEffectiveType(TransactionType.TRANSFER, AccountType.CHECKING),
        )
    }

    @Test
    fun defaultCardChargeStatus_todayOrPastPosted_futureScheduled() {
        val today = millis(LocalDate(2026, 3, 15))

        assertEquals(TransactionStatus.CONFIRMED, defaultCardChargeStatus(millis(LocalDate(2026, 3, 14)), today, tz))
        assertEquals(TransactionStatus.CONFIRMED, defaultCardChargeStatus(today, today, tz))
        assertEquals(TransactionStatus.PENDING, defaultCardChargeStatus(millis(LocalDate(2026, 3, 16)), today, tz))
    }

    @Test
    fun cardStatementContextLine_shownOnlyWhenBothCycleDaysSet() {
        val complete = summary(statementClosingDay = 28, paymentDueDay = 15)
        assertEquals(
            "Bills to statement closing Mar 28 · due Apr 15",
            cardStatementContextLine(complete, millis(LocalDate(2026, 3, 10)), tz),
        )

        assertNull(cardStatementContextLine(summary(statementClosingDay = null, paymentDueDay = 15), millis(LocalDate(2026, 3, 10)), tz))
        assertNull(cardStatementContextLine(summary(statementClosingDay = 28, paymentDueDay = null), millis(LocalDate(2026, 3, 10)), tz))
    }

    private fun summary(statementClosingDay: Int?, paymentDueDay: Int?): CreditCardSummary {
        return CreditCardSummary(
            accountId = 1L,
            accountName = "Card",
            currentBalance = 0L,
            reservedAmount = 0L,
            statementBalance = null,
            creditLimit = null,
            availableCredit = null,
            dueDate = null,
            statementClosingDay = statementClosingDay,
            paymentDueDay = paymentDueDay,
        )
    }

    private fun millis(date: LocalDate): Long = date.atStartOfDayIn(tz).toEpochMilliseconds()
}
