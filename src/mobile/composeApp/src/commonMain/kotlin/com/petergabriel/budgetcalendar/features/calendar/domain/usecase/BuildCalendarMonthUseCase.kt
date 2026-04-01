package com.petergabriel.budgetcalendar.features.calendar.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarDay
import com.petergabriel.budgetcalendar.features.calendar.domain.model.CalendarMonth
import com.petergabriel.budgetcalendar.features.calendar.domain.model.contains
import com.petergabriel.budgetcalendar.features.calendar.domain.model.firstDay
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class BuildCalendarMonthUseCase(
    private val calculateDaySummaryUseCase: CalculateDaySummaryUseCase,
) {
    operator fun invoke(
        yearMonth: YearMonth,
        transactions: List<Transaction>,
        selectedDate: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        nowMillisProvider: () -> Long = { DateUtils.nowMillis() },
    ): CalendarMonth {
        val visibleTransactions = transactions.filterNot { transaction ->
            transaction.status == TransactionStatus.CANCELLED
        }
        val transactionsByDate = visibleTransactions.groupBy { transaction ->
            Instant
                .fromEpochMilliseconds(transaction.date)
                .toLocalDateTime(timeZone)
                .date
        }
        val daySummaries = calculateDaySummaryUseCase(visibleTransactions, timeZone)

        val firstDayOfMonth = yearMonth.firstDay()
        val leadingDaysCount = firstDayOfMonth.dayOfWeek.daysSinceSunday()
        val totalDaysInMonth = firstDayOfMonth
            .plus(DatePeriod(months = 1))
            .minus(DatePeriod(days = 1))
            .day

        val visibleGridCells = leadingDaysCount + totalDaysInMonth
        val trailingDaysCount = (7 - (visibleGridCells % 7)) % 7
        val totalCells = visibleGridCells + trailingDaysCount

        val firstGridDate = firstDayOfMonth.minus(DatePeriod(days = leadingDaysCount))
        val today = Instant
            .fromEpochMilliseconds(nowMillisProvider())
            .toLocalDateTime(timeZone)
            .date

        val days = (0 until totalCells).map { index ->
            val cellDate = firstGridDate.plus(DatePeriod(days = index))
            CalendarDay(
                date = cellDate,
                isCurrentMonth = yearMonth.contains(cellDate),
                isToday = cellDate == today,
                isSelected = cellDate == selectedDate,
                dailySummary = daySummaries[cellDate],
            )
        }

        return CalendarMonth(
            yearMonth = yearMonth,
            days = days,
            transactionsByDate = transactionsByDate,
        )
    }

    private fun DayOfWeek.daysSinceSunday(): Int {
        return if (this == DayOfWeek.SUNDAY) {
            0
        } else {
            ordinal + 1
        }
    }
}
