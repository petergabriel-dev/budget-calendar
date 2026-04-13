package com.petergabriel.budgetcalendar.features.recurring.domain.usecase

import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurrenceType
import com.petergabriel.budgetcalendar.features.recurring.domain.repository.IRecurringTransactionRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class GenerateMonthlyTransactionsUseCase(
    private val recurringRepository: IRecurringTransactionRepository,
    private val transactionRepository: ITransactionRepository,
) {
    suspend operator fun invoke(
        monthsAhead: Int = 0,
        startMonthOffset: Int = 0,
        nowMillis: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Result<List<Transaction>> {
        if (monthsAhead < 0) {
            return Result.failure(IllegalArgumentException("monthsAhead must be >= 0"))
        }

        if (startMonthOffset < 0) {
            return Result.failure(IllegalArgumentException("startMonthOffset must be >= 0"))
        }

        return runCatching {
            val activeRecurring = recurringRepository.getActive().first()
            if (activeRecurring.isEmpty()) {
                return@runCatching emptyList()
            }

            val monthStart = RecurringGenerationUtils.currentMonthStart(nowMillis = nowMillis, timeZone = timeZone)
            val created = mutableListOf<Transaction>()

            for (monthOffset in startMonthOffset..(startMonthOffset + monthsAhead)) {
                val (monthStartMillis, monthEndMillis) = RecurringGenerationUtils.monthBounds(
                    monthStart = monthStart,
                    monthOffset = monthOffset,
                    timeZone = timeZone,
                )

                val pendingRecurringIds = transactionRepository
                    .getTransactionsByDateRange(monthStartMillis, monthEndMillis)
                    .first()
                    .asSequence()
                    .filter { transaction -> transaction.status == TransactionStatus.PENDING }
                    .mapNotNull { transaction -> RecurringGenerationUtils.extractRecurringId(transaction.description) }
                    .toSet()

                activeRecurring.forEach { recurring ->
                    if (recurring.id in pendingRecurringIds) {
                        return@forEach
                    }

                    val occurrenceDate = RecurringGenerationUtils.occurrenceDate(
                        monthStart = monthStart,
                        monthOffset = monthOffset,
                        dayOfMonth = recurring.dayOfMonth,
                    )
                    val occurrenceMillis = occurrenceDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
                    created += createGeneratedTransaction(recurring = recurring, dateMillis = occurrenceMillis)
                }
            }

            created
        }
    }

    private suspend fun createGeneratedTransaction(
        recurring: RecurringTransaction,
        dateMillis: Long,
    ): List<Transaction> {
        val description = RecurringGenerationUtils.buildGeneratedDescription(
            recurringId = recurring.id,
            description = recurring.description,
        )

        if (recurring.type != RecurrenceType.TRANSFER) {
            val generated = transactionRepository.createTransaction(
                CreateTransactionRequest(
                    accountId = recurring.accountId,
                    destinationAccountId = null,
                    amount = recurring.amount,
                    date = dateMillis,
                    type = RecurringGenerationUtils.toTransactionType(recurring.type),
                    status = TransactionStatus.PENDING,
                    description = description,
                    category = null,
                    linkedTransactionId = null,
                    signedAmount = if (recurring.type == RecurrenceType.INCOME) recurring.amount else -recurring.amount,
                    isSandbox = false,
                ),
            )
            return listOf(generated)
        }

        val destinationId = checkNotNull(recurring.destinationAccountId) {
            "Transfer recurring transaction #${recurring.id} is missing destination account"
        }

        val source = transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = recurring.accountId,
                destinationAccountId = destinationId,
                amount = recurring.amount,
                date = dateMillis,
                type = RecurringGenerationUtils.toTransactionType(recurring.type),
                status = TransactionStatus.PENDING,
                description = description,
                category = null,
                linkedTransactionId = null,
                signedAmount = -recurring.amount,
                isSandbox = false,
            ),
        )

        val destination = transactionRepository.createTransaction(
            CreateTransactionRequest(
                accountId = destinationId,
                destinationAccountId = recurring.accountId,
                amount = recurring.amount,
                date = dateMillis,
                type = RecurringGenerationUtils.toTransactionType(recurring.type),
                status = TransactionStatus.PENDING,
                description = description,
                category = null,
                linkedTransactionId = source.id,
                signedAmount = recurring.amount,
                isSandbox = false,
            ),
        )

        transactionRepository.updateLinkedTransactionId(source.id, destination.id)

        return listOf(source.copy(linkedTransactionId = destination.id), destination)
    }
}
