package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IMonthlyRolloverRepository

class SaveMonthlyRolloverUseCase(
    private val monthlyRolloverRepository: IMonthlyRolloverRepository,
) {
    suspend operator fun invoke(year: Int, month: Int, amount: Long): Result<MonthlyRollover> {
        if (year < 2000) {
            return Result.failure(IllegalArgumentException("Year must be 2000 or later"))
        }

        if (month !in 1..12) {
            return Result.failure(IllegalArgumentException("Month must be between 1 and 12"))
        }

        return monthlyRolloverRepository.saveRollover(year = year, month = month, amount = amount)
    }
}
