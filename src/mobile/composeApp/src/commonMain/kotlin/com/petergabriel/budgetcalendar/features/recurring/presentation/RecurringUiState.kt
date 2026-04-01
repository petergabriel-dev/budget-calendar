package com.petergabriel.budgetcalendar.features.recurring.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.recurring.domain.model.GeneratedTransaction
import com.petergabriel.budgetcalendar.features.recurring.domain.model.RecurringTransaction

data class RecurringUiState(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val upcomingGenerated: List<GeneratedTransaction> = emptyList(),
    val availableAccounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showForm: Boolean = false,
    val editingRecurring: RecurringTransaction? = null,
)
