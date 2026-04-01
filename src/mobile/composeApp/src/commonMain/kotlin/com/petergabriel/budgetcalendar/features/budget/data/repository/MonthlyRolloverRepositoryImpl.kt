package com.petergabriel.budgetcalendar.features.budget.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.data.mapper.BudgetMapper
import com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IMonthlyRolloverRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class MonthlyRolloverRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val budgetMapper: BudgetMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IMonthlyRolloverRepository {

    override suspend fun getRolloverForMonth(year: Int, month: Int): Result<MonthlyRollover?> {
        return runCatching {
            database.monthly_rolloversQueries
                .getRolloverByMonth(year.toLong(), month.toLong(), budgetMapper::toMonthlyRollover)
                .executeAsOneOrNull()
        }
    }

    override fun getAllRollovers(): Flow<List<MonthlyRollover>> {
        return database.monthly_rolloversQueries
            .getAllRollovers(budgetMapper::toMonthlyRollover)
            .asFlow()
            .mapToList(dispatcher)
    }

    override suspend fun saveRollover(year: Int, month: Int, amount: Long): Result<MonthlyRollover> {
        return runCatching {
            val now = DateUtils.nowMillis()
            database.monthly_rolloversQueries.upsertRollover(
                year.toLong(),
                month.toLong(),
                amount,
                now,
            )

            checkNotNull(
                database.monthly_rolloversQueries
                    .getRolloverByMonth(year.toLong(), month.toLong(), budgetMapper::toMonthlyRollover)
                    .executeAsOneOrNull(),
            ) {
                "Failed to load rollover after upsert"
            }
        }
    }
}
