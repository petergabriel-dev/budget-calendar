package com.petergabriel.budgetcalendar.features.creditcard.domain.usecase

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class GetCreditCardSummariesUseCase(
    private val creditCardRepository: ICreditCardRepository,
    private val accountRepository: IAccountRepository,
    private val calculateReservedAmountUseCase: CalculateReservedAmountUseCase,
) {
    operator fun invoke(): Flow<List<CreditCardSummary>> {
        return creditCardRepository.getAllSettings()
            .map { settings ->
                settings.mapNotNull { setting ->
                    val account = accountRepository.getAccountById(setting.accountId)
                    if (account == null || account.type != AccountType.CREDIT_CARD) {
                        null
                    } else {
                        val reservedAmount = calculateReservedAmountUseCase(account.id).getOrElse { 0L }
                        val availableCredit = setting.creditLimit?.minus(abs(account.balance))

                        CreditCardSummary(
                            accountId = account.id,
                            accountName = account.name,
                            currentBalance = account.balance,
                            reservedAmount = reservedAmount,
                            statementBalance = setting.statementBalance,
                            creditLimit = setting.creditLimit,
                            availableCredit = availableCredit,
                            dueDate = setting.dueDate,
                        )
                    }
                }.sortedBy { summary -> summary.accountName.lowercase() }
            }
    }
}
