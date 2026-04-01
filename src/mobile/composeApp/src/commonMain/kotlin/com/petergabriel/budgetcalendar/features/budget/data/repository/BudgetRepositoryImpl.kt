package com.petergabriel.budgetcalendar.features.budget.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.features.budget.data.mapper.BudgetMapper
import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import com.petergabriel.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class BudgetRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val budgetMapper: BudgetMapper,
    private val transactionRepository: ITransactionRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IBudgetRepository {

    private val transactionChangedTrigger = transactionRepository.transactionChangedTrigger.onStart { emit(Unit) }

    override fun getTotalSpendingPoolBalance(): Flow<Long> {
        val totalFlow = database.budgetQueries
            .getTotalSpendingPoolBalance()
            .asFlow()
            .mapToOne(dispatcher)
            .map(budgetMapper::toAmount)
        return totalFlow.combine(transactionChangedTrigger) { total, _ -> total }
    }

    override fun getPendingReservations(): Flow<Long> {
        val pendingFlow = database.budgetQueries
            .getPendingAndOverdueForSpendingPool(budgetMapper::toPendingAndOverdueRow)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows
                    .filter { row -> row.status.equals("pending", ignoreCase = true) }
                    .sumOf { row -> row.amount }
            }
        return pendingFlow.combine(transactionChangedTrigger) { pending, _ -> pending }
    }

    override fun getOverdueReservations(): Flow<Long> {
        val overdueFlow = database.budgetQueries
            .getPendingAndOverdueForSpendingPool(budgetMapper::toPendingAndOverdueRow)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows
                    .filter { row -> row.status.equals("overdue", ignoreCase = true) }
                    .sumOf { row -> row.amount }
            }
        return overdueFlow.combine(transactionChangedTrigger) { overdue, _ -> overdue }
    }

    override fun getCreditCardReservedAmount(): Flow<Long> {
        val reservedFlow = database.budgetQueries
            .getCreditCardReservedAmount()
            .asFlow()
            .mapToOne(dispatcher)
            .map(budgetMapper::toAmount)
        return reservedFlow.combine(transactionChangedTrigger) { reserved, _ -> reserved }
    }

    override fun getCreditCardReservations(): Flow<List<CreditCardReservation>> {
        val reservationsFlow = database.budgetQueries
            .getCreditCardReservations(budgetMapper::toCreditCardReservation)
            .asFlow()
            .mapToList(dispatcher)
        return reservationsFlow.combine(transactionChangedTrigger) { reservations, _ -> reservations }
    }
}
