package com.petergabriel.budgetcalendar.features.calendar.domain.usecase

import com.petergabriel.budgetcalendar.features.calendar.domain.model.firstDay
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class GetMonthTransactionsUseCase(
    private val transactionRepository: ITransactionRepository,
) {
    operator fun invoke(
        yearMonth: YearMonth,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Flow<List<Transaction>> {
        val firstDayOfMonth = yearMonth.firstDay()
        val monthStartMillis = firstDayOfMonth.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val nextMonthStartMillis = firstDayOfMonth
            .plus(DatePeriod(months = 1))
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

        return transactionRepository.getTransactionsByDateRange(
            startDate = monthStartMillis,
            endDate = nextMonthStartMillis - 1,
        )
    }
}
