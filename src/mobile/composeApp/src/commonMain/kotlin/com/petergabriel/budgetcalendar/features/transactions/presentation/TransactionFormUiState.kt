package com.petergabriel.budgetcalendar.features.transactions.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

data class TransactionFormUiState(
    val availableAccounts: List<Account> = emptyList(),
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
)
