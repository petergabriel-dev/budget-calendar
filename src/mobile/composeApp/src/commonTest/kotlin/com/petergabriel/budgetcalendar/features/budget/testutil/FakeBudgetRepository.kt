package com.petergabriel.budgetcalendar.features.budget.testutil

import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBudgetRepository : IBudgetRepository {
    private val totalSpendingPoolBalance = MutableStateFlow(0L)
    private val pendingReservations = MutableStateFlow(0L)
    private val overdueReservations = MutableStateFlow(0L)
    private val creditCardReservedAmount = MutableStateFlow(0L)

    override fun getTotalSpendingPoolBalance(): Flow<Long> = totalSpendingPoolBalance

    override fun getPendingReservations(): Flow<Long> = pendingReservations

    override fun getOverdueReservations(): Flow<Long> = overdueReservations

    override fun getCreditCardReservedAmount(): Flow<Long> = creditCardReservedAmount

    fun setTotalSpendingPoolBalance(amount: Long) {
        totalSpendingPoolBalance.value = amount
    }

    fun setPendingReservations(amount: Long) {
        pendingReservations.value = amount
    }

    fun setOverdueReservations(amount: Long) {
        overdueReservations.value = amount
    }

    fun setCreditCardReservedAmount(amount: Long) {
        creditCardReservedAmount.value = amount
    }
}
