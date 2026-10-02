package com.petergabriel.budgetcalendar.features.budget.domain.repository

import kotlinx.coroutines.flow.Flow

interface IBudgetRepository {
    fun getTotalSpendingPoolBalance(): Flow<Long>
    fun getPendingReservations(): Flow<Long>
    fun getOverdueReservations(): Flow<Long>
    fun getCreditCardReservedAmount(): Flow<Long>
}
