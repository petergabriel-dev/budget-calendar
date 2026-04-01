package com.petergabriel.budgetcalendar.features.budget.domain.usecase

import com.petergabriel.budgetcalendar.features.budget.domain.model.MonthlyRollover
import com.petergabriel.budgetcalendar.features.budget.domain.repository.IMonthlyRolloverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRolloverHistoryUseCase(
    private val monthlyRolloverRepository: IMonthlyRolloverRepository,
) {
    operator fun invoke(): Flow<List<MonthlyRollover>> {
        return monthlyRolloverRepository.getAllRollovers().map { items ->
            items.sortedWith(
                compareByDescending<MonthlyRollover> { rollover -> rollover.year }
                    .thenByDescending { rollover -> rollover.month },
            )
        }
    }
}
