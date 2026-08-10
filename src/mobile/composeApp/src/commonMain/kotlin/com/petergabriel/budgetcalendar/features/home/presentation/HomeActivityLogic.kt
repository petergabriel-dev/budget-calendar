package com.petergabriel.budgetcalendar.features.home.presentation

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.home.presentation.components.AmountTone
import com.petergabriel.budgetcalendar.features.home.presentation.components.SelectedDayTransactionPreviewItem
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal fun homeWeekDates(today: LocalDate): List<LocalDate> {
    val weekStart = today - DatePeriod(days = today.dayOfWeek.ordinal)
    return (0..6).map { offset -> weekStart + DatePeriod(days = offset) }
}

internal fun transactionLocalDate(transaction: Transaction, timeZone: TimeZone): LocalDate {
    return Instant
        .fromEpochMilliseconds(transaction.date)
        .toLocalDateTime(timeZone)
        .date
}

internal fun weeklyExpenseTotal(
    transactions: List<Transaction>,
    weekStart: LocalDate,
    timeZone: TimeZone,
): Long {
    val weekEnd = weekStart + DatePeriod(days = 7)
    return transactions
        .asSequence()
        .filter { transaction ->
            transaction.type == TransactionType.EXPENSE &&
                transaction.status != TransactionStatus.CANCELLED
        }
        .filter { transaction ->
            val date = transactionLocalDate(transaction, timeZone)
            date >= weekStart && date < weekEnd
        }
        .sumOf(Transaction::amount)
}

internal fun selectedDayTransactions(
    transactions: List<Transaction>,
    selectedDate: LocalDate,
    timeZone: TimeZone,
): List<Transaction> {
    return transactions
        .asSequence()
        .filter { transaction -> transaction.status != TransactionStatus.CANCELLED }
        .filter { transaction -> transactionLocalDate(transaction, timeZone) == selectedDate }
        .sortedWith(compareBy<Transaction> { it.date }.thenByDescending { it.createdAt })
        .toList()
}

internal fun orderedOverdueTransactions(transactions: List<Transaction>): List<Transaction> {
    return transactions
        .asSequence()
        .filter { transaction -> transaction.status == TransactionStatus.OVERDUE }
        .sortedWith(compareBy<Transaction> { it.date }.thenBy { it.createdAt })
        .toList()
}

internal fun previewItem(
    transaction: Transaction,
    selectedDate: LocalDate,
    accountNames: Map<Long, String>,
): SelectedDayTransactionPreviewItem {
    val title = if (transaction.type == TransactionType.TRANSFER) {
        "Transfer"
    } else {
        transaction.category?.takeIf(String::isNotBlank) ?: "Uncategorized"
    }
    val accountName = accountNames[transaction.accountId] ?: "Account #${transaction.accountId}"
    val destinationName = transaction.destinationAccountId?.let { id ->
        accountNames[id] ?: "Account #$id"
    }
    val accountLabel = if (transaction.type == TransactionType.TRANSFER && destinationName != null) {
        "$accountName → $destinationName"
    } else {
        accountName
    }
    val amountLabel = CurrencyUtils.formatCents(
        amountInCents = if (transaction.type == TransactionType.TRANSFER) transaction.amount else transaction.signedAmount,
        includePlusSign = transaction.type != TransactionType.TRANSFER && transaction.signedAmount >= 0,
    )
    val amountTone = when (transaction.type) {
        TransactionType.INCOME -> AmountTone.Positive
        TransactionType.EXPENSE -> AmountTone.Negative
        TransactionType.TRANSFER -> AmountTone.Info
    }
    val statusLabel = transaction.status.name.lowercase().replaceFirstChar { character -> character.titlecase() }
    return SelectedDayTransactionPreviewItem(
        id = transaction.id,
        title = title,
        description = transaction.description,
        accountLabel = accountLabel,
        amountLabel = amountLabel,
        amountTone = amountTone,
        statusLabel = statusLabel,
        accessibilityLabel = "$title, $accountLabel, $amountLabel, $statusLabel, ${homeDateLabel(selectedDate)}",
    )
}

internal fun relativeHomeDateLabel(transactionDate: LocalDate, today: LocalDate): String {
    return when (val daysAgo = today.toEpochDays() - transactionDate.toEpochDays()) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> "$daysAgo days ago"
    }
}

internal fun homeDateLabel(date: LocalDate): String {
    val weekday = date.dayOfWeek.name.lowercase().replaceFirstChar { character -> character.titlecase() }
    val month = date.month.name.lowercase().replaceFirstChar { character -> character.titlecase() }.take(3)
    return "$weekday, $month ${date.dayOfMonth}"
}
