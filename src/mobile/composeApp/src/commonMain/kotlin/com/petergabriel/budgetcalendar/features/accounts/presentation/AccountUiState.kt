package com.petergabriel.budgetcalendar.features.accounts.presentation

import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account

data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val balances: Map<Long, Long> = emptyMap(),
    val netWorth: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showForm: Boolean = false,
    val editingAccount: Account? = null,
)
