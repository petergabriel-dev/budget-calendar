package com.petergabriel.budgetcalendar.features.sandbox.domain.usecase

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.repository.ISandboxRepository

class CheckAndExpireSandboxesUseCase(
    private val sandboxRepository: ISandboxRepository,
) {
    suspend operator fun invoke(): Result<Int> {
        val cutoff = DateUtils.nowMillis() - (30L * DateUtils.MILLIS_IN_DAY)
        return sandboxRepository.deleteExpiredSnapshots(cutoff)
    }
}
