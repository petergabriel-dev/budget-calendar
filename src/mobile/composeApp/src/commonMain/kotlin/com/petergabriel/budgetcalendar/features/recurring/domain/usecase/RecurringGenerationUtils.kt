package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min
import kotlin.time.Instant

internal object RecurringGenerationUtils {
    private const val GENERATED_PREFIX = "[recurring:"

    fun toTransactionType(type: RecurrenceType): TransactionType {
        return when (type) {
            RecurrenceType.INCOME -> TransactionType.INCOME
            RecurrenceType.EXPENSE -> TransactionType.EXPENSE
            RecurrenceType.TRANSFER -> TransactionType.TRANSFER
        }
    }

    fun buildGeneratedDescription(recurringId: Long, description: String?): String {
        val prefix = "$GENERATED_PREFIX$recurringId]"
        val body = description?.trim().orEmpty()
        return if (body.isEmpty()) {
            prefix
        } else {
            "$prefix $body"
        }
    }

    fun extractRecurringId(description: String?): Long? {
        if (description.isNullOrBlank() || !description.startsWith(GENERATED_PREFIX)) {
            return null
        }

        val endBracketIndex = description.indexOf(']')
        if (endBracketIndex <= GENERATED_PREFIX.length) {
            return null
        }

        return description
            .substring(GENERATED_PREFIX.length, endBracketIndex)
            .toLongOrNull()
    }

    fun currentMonthStart(nowMillis: Long, timeZone: TimeZone): LocalDate {
        val localDate = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        return LocalDate(localDate.year, localDate.month.number, 1)
    }

    fun occurrenceDate(monthStart: LocalDate, monthOffset: Int, dayOfMonth: Int): LocalDate {
        val targetMonthStart = monthStart.plus(DatePeriod(months = monthOffset))
        val lastDay = targetMonthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).day
        val clampedDay = min(dayOfMonth, lastDay)
        return LocalDate(targetMonthStart.year, targetMonthStart.month.number, clampedDay)
    }

    fun monthBounds(monthStart: LocalDate, monthOffset: Int, timeZone: TimeZone): Pair<Long, Long> {
        val targetStart = monthStart.plus(DatePeriod(months = monthOffset))
        val targetEnd = targetStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

        val startMillis = targetStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endMillis = targetEnd.atStartOfDayIn(timeZone).toEpochMilliseconds() + 86_399_999L
        return startMillis to endMillis
    }
}
