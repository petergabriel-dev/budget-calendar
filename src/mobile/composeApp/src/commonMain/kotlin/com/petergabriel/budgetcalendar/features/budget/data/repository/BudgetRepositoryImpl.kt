package com.petergabriel.budgetcalendar.features.budget.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.features.budget.data.mapper.BudgetMapper
import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IBudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val budgetMapper: BudgetMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IBudgetRepository {

    override fun getTotalSpendingPoolBalance(): Flow<Long> {
        return database.budgetQueries
            .getTotalSpendingPoolBalance()
            .asFlow()
            .mapToOne(dispatcher)
            .map(budgetMapper::toAmount)
    }

    override fun getPendingReservations(): Flow<Long> {
        return database.budgetQueries
            .getPendingAndOverdueForSpendingPool(budgetMapper::toPendingAndOverdueRow)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows
                    .filter { row -> row.status.equals("pending", ignoreCase = true) }
                    .sumOf { row -> row.amount }
            }
    }

    override fun getOverdueReservations(): Flow<Long> {
        return database.budgetQueries
            .getPendingAndOverdueForSpendingPool(budgetMapper::toPendingAndOverdueRow)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows ->
                rows
                    .filter { row -> row.status.equals("overdue", ignoreCase = true) }
                    .sumOf { row -> row.amount }
            }
    }

    override fun getCreditCardReservedAmount(): Flow<Long> {
        return database.budgetQueries
            .getCreditCardReservedAmount()
            .asFlow()
            .mapToOne(dispatcher)
            .map(budgetMapper::toAmount)
    }

    override fun getCreditCardReservations(): Flow<List<CreditCardReservation>> {
        return database.budgetQueries
            .getCreditCardReservations(budgetMapper::toCreditCardReservation)
            .asFlow()
            .mapToList(dispatcher)
    }
}
