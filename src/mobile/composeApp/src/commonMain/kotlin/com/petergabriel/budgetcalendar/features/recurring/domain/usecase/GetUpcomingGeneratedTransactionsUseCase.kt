package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.GeneratedTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GetUpcomingGeneratedTransactionsUseCase(
    private val recurringRepository: IRecurringTransactionRepository,
    private val transactionRepository: ITransactionRepository,
) {
    operator fun invoke(
        monthsAhead: Int,
        nowMillis: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Flow<List<GeneratedTransaction>> {
        if (monthsAhead <= 0) {
            return flowOf(emptyList())
        }

        val monthStart = RecurringGenerationUtils.currentMonthStart(nowMillis = nowMillis, timeZone = timeZone)
        val (windowStart, _) = RecurringGenerationUtils.monthBounds(
            monthStart = monthStart,
            monthOffset = 0,
            timeZone = timeZone,
        )
        val (_, windowEnd) = RecurringGenerationUtils.monthBounds(
            monthStart = monthStart,
            monthOffset = monthsAhead - 1,
            timeZone = timeZone,
        )

        return combine(
            recurringRepository.getActive(),
            transactionRepository.getTransactionsByDateRange(windowStart, windowEnd),
        ) { recurringTransactions, monthTransactions ->
            val existingByKey = monthTransactions
                .asSequence()
                .filter { transaction -> transaction.status == TransactionStatus.PENDING }
                .mapNotNull { transaction ->
                    val recurringId = RecurringGenerationUtils.extractRecurringId(transaction.description) ?: return@mapNotNull null
                    val localDate = Instant
                        .fromEpochMilliseconds(transaction.date)
                        .toLocalDateTime(timeZone)
                        .date
                    OccurrenceKey(
                        recurringId = recurringId,
                        year = localDate.year,
                        month = localDate.month.number,
                    )
                }
                .toSet()

            val generated = mutableListOf<GeneratedTransaction>()
            recurringTransactions.forEach { recurring ->
                for (monthOffset in 0 until monthsAhead) {
                    val occurrenceDate = RecurringGenerationUtils.occurrenceDate(
                        monthStart = monthStart,
                        monthOffset = monthOffset,
                        dayOfMonth = recurring.dayOfMonth,
                    )
                    val occurrenceMillis = occurrenceDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val key = OccurrenceKey(
                        recurringId = recurring.id,
                        year = occurrenceDate.year,
                        month = occurrenceDate.month.number,
                    )

                    generated += GeneratedTransaction(
                        recurringId = recurring.id,
                        date = occurrenceMillis,
                        amount = recurring.amount,
                        type = recurring.type,
                        accountId = recurring.accountId,
                        willGenerate = key !in existingByKey,
                    )
                }
            }

            generated.sortedBy(GeneratedTransaction::date)
        }
    }

    private data class OccurrenceKey(
        val recurringId: Long,
        val year: Int,
        val month: Int,
    )
}
