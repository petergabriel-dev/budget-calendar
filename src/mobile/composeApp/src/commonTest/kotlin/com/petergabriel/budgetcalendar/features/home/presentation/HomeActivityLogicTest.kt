package com.petergabriel.budgetcalendar.features.home.presentation

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeActivityLogicTest {
    private val timeZone = TimeZone.UTC

    @Test
    fun homeWeekDates_returnsSevenChronologicalDays() {
        val dates = homeWeekDates(LocalDate(2026, 8, 12))

        assertEquals(7, dates.size)
        assertEquals(LocalDate(2026, 8, 10), dates.first())
        assertEquals(LocalDate(2026, 8, 16), dates.last())
    }

    @Test
    fun weeklyExpenseTotal_excludesCancelledAndOutsideWeek() {
        val weekStart = LocalDate(2026, 8, 10)
        val transactions = listOf(
            transaction(1L, LocalDate(2026, 8, 10), amount = 1_000L),
            transaction(2L, LocalDate(2026, 8, 16), amount = 2_000L),
            transaction(3L, LocalDate(2026, 8, 17), amount = 4_000L),
            transaction(4L, LocalDate(2026, 8, 11), amount = 8_000L, status = TransactionStatus.CANCELLED),
            transaction(5L, LocalDate(2026, 8, 12), amount = 3_000L, type = TransactionType.INCOME),
        )

        assertEquals(3_000L, weeklyExpenseTotal(transactions, weekStart, timeZone))
    }

    @Test
    fun orderedOverdueTransactions_sortsOldestFirstAndExcludesOtherStatuses() {
        val transactions = listOf(
            transaction(1L, LocalDate(2026, 8, 12), status = TransactionStatus.OVERDUE),
            transaction(2L, LocalDate(2026, 8, 10), status = TransactionStatus.OVERDUE),
            transaction(3L, LocalDate(2026, 8, 9), status = TransactionStatus.PENDING),
        )

        assertEquals(
            listOf(2L, 1L),
            orderedOverdueTransactions(transactions).map(Transaction::id),
        )
    }

    @Test
    fun selectedDayTransactions_filtersCancelledAndSortsByDateThenCreatedAt() {
        val selectedDate = LocalDate(2026, 8, 12)
        val transactions = listOf(
            transaction(1L, selectedDate, createdAt = 10L),
            transaction(2L, selectedDate, createdAt = 20L),
            transaction(3L, selectedDate - DatePeriod(days = 1), createdAt = 30L),
            transaction(4L, selectedDate, createdAt = 40L, status = TransactionStatus.CANCELLED),
        )

        assertEquals(
            listOf(2L, 1L),
            selectedDayTransactions(transactions, selectedDate, timeZone).map(Transaction::id),
        )
    }

    private fun transaction(
        id: Long,
        date: LocalDate,
        amount: Long = 1_000L,
        createdAt: Long = id,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
        type: TransactionType = TransactionType.EXPENSE,
    ): Transaction {
        return Transaction(
            id = id,
            accountId = 1L,
            destinationAccountId = null,
            amount = amount,
            signedAmount = if (type == TransactionType.INCOME) amount else -amount,
            date = date.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            type = type,
            status = status,
            description = null,
            category = "Test",
            linkedTransactionId = null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
