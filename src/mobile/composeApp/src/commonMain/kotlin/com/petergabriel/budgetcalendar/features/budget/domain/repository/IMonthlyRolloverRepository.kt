package com.petergabriel.budgetcalendar.features.budget.domain.repository

import com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover
import kotlinx.coroutines.flow.Flow

interface IMonthlyRolloverRepository {
    suspend fun getRolloverForMonth(year: Int, month: Int): Result<MonthlyRollover?>
    fun getAllRollovers(): Flow<List<MonthlyRollover>>
    suspend fun saveRollover(year: Int, month: Int, amount: Long): Result<MonthlyRollover>
}
