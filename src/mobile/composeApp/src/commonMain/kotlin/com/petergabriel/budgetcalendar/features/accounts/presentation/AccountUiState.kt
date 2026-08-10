package com.petergabriel.budgetcalendar.features.accounts.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account

sealed interface AccountEffect {
    data class DeleteSucceeded(val accountId: Long) : AccountEffect
}

data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val balances: Map<Long, Long> = emptyMap(),
    val netWorth: Long? = null,
    val isNetWorthLoading: Boolean = true,
    val netWorthError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val isDeleting: Boolean = false,
    val mutationError: String? = null,
    val showForm: Boolean = false,
    val editingAccount: Account? = null,
)
