package com.petergabriel.budgetcalendar.features.budget.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IMonthlyRolloverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMonthlyRolloverRepository : IMonthlyRolloverRepository {
    private val rollovers = mutableListOf<MonthlyRollover>()
    private val rolloversFlow = MutableStateFlow<List<MonthlyRollover>>(emptyList())
    private var nextId = 1L

    override suspend fun getRolloverForMonth(year: Int, month: Int): Result<MonthlyRollover?> {
        return Result.success(rollovers.firstOrNull { rollover -> rollover.year == year && rollover.month == month })
    }

    override fun getAllRollovers(): Flow<List<MonthlyRollover>> = rolloversFlow

    override suspend fun saveRollover(year: Int, month: Int, amount: Long): Result<MonthlyRollover> {
        val now = DateUtils.nowMillis()
        val existingIndex = rollovers.indexOfFirst { rollover -> rollover.year == year && rollover.month == month }

        val saved = if (existingIndex >= 0) {
            rollovers[existingIndex].copy(
                rolloverAmount = amount,
                createdAt = now,
            ).also { updated ->
                rollovers[existingIndex] = updated
            }
        } else {
            MonthlyRollover(
                id = nextId++,
                year = year,
                month = month,
                rolloverAmount = amount,
                createdAt = now,
            ).also { created ->
                rollovers += created
            }
        }

        emitRollovers()
        return Result.success(saved)
    }

    fun setRollovers(items: List<MonthlyRollover>) {
        rollovers.clear()
        rollovers += items
        nextId = (items.maxOfOrNull { rollover -> rollover.id } ?: 0L) + 1L
        emitRollovers()
    }

    fun currentRollovers(): List<MonthlyRollover> = rollovers.toList()

    private fun emitRollovers() {
        rolloversFlow.value = rollovers.sortedWith(
            compareByDescending<MonthlyRollover> { rollover -> rollover.year }
                .thenByDescending { rollover -> rollover.month },
        )
    }
}
