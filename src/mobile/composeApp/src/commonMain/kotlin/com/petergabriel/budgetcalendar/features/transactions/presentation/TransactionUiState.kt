package com.petergabriel.budgetcalendar.features.transactions.presentation

import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType

data class DateRange(
    val startDate: Long,
    val endDate: Long,
)

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedTransaction: Transaction? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDateFilter: DateRange? = null,
    val typeFilter: TransactionType? = null,
)
