package com.petergabriel.budgetcalendar.features.budget.domain.repository

import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation
import kotlinx.coroutines.flow.Flow

interface IBudgetRepository {
    fun getTotalSpendingPoolBalance(): Flow<Long>
    fun getPendingReservations(): Flow<Long>
    fun getOverdueReservations(): Flow<Long>
    fun getCreditCardReservedAmount(): Flow<Long>
    fun getCreditCardReservations(): Flow<List<CreditCardReservation>>
}
