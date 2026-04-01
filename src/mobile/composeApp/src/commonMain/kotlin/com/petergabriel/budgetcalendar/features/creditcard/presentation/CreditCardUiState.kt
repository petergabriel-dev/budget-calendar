package com.petergabriel.budgetcalendar.features.creditcard.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction

data class CreditCardUiState(
    val creditCards: List<CreditCardSummary> = emptyList(),
    val selectedCard: CreditCardSummary? = null,
    val selectedCardSettings: CreditCardSettings? = null,
    val selectedCardTransactions: List<Transaction> = emptyList(),
    val spendingPoolAccounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val isPaymentSheetVisible: Boolean = false,
    val suggestedPaymentAmount: Long = 0L,
    val error: String? = null,
)
