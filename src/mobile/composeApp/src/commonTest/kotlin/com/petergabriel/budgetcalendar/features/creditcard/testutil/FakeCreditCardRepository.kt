package com.petergabriel.budgetcalendar.features.creditcard.testutil

import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCreditCardRepository : ICreditCardRepository {
    private val settings = mutableListOf<CreditCardSettings>()
    private val settingsFlow = MutableStateFlow<List<CreditCardSettings>>(emptyList())
    private val reservedAmounts = mutableMapOf<Long, Long>()
    private var nextId = 1L

    override fun getAllSettings(): Flow<List<CreditCardSettings>> = settingsFlow

    override suspend fun getSettingsByAccountId(accountId: Long): Result<CreditCardSettings> {
        val settings = settings.firstOrNull { item -> item.accountId == accountId }
            ?: return Result.failure(NoSuchElementException("Credit card settings for accountId=$accountId were not found"))
        return Result.success(settings)
    }

    override suspend fun insert(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
    ): Result<CreditCardSettings> {
        if (settings.any { item -> item.accountId == accountId }) {
            return Result.failure(IllegalStateException("Credit card settings for accountId=$accountId already exist"))
        }

        val now = DateUtils.nowMillis()
        val created = CreditCardSettings(
            id = nextId++,
            accountId = accountId,
            creditLimit = creditLimit,
            statementBalance = statementBalance,
            dueDate = dueDate,
            createdAt = now,
            updatedAt = now,
        )
        settings += created
        emitSettings()
        return Result.success(created)
    }

    override suspend fun update(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
    ): Result<CreditCardSettings> {
        val index = settings.indexOfFirst { item -> item.accountId == accountId }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Credit card settings for accountId=$accountId were not found"))
        }

        val updated = settings[index].copy(
            creditLimit = creditLimit,
            statementBalance = statementBalance,
            dueDate = dueDate,
            updatedAt = DateUtils.nowMillis(),
        )
        settings[index] = updated
        emitSettings()
        return Result.success(updated)
    }

    override suspend fun delete(accountId: Long): Result<Unit> {
        settings.removeAll { item -> item.accountId == accountId }
        reservedAmounts.remove(accountId)
        emitSettings()
        return Result.success(Unit)
    }

    override suspend fun getReservedAmount(accountId: Long): Result<Long> {
        return Result.success(reservedAmounts[accountId] ?: 0L)
    }

    override suspend fun getAllReservedAmounts(): Result<Map<Long, Long>> {
        return Result.success(reservedAmounts.toMap())
    }

    fun setReservedAmount(accountId: Long, amount: Long) {
        reservedAmounts[accountId] = amount
    }

    fun seedSettings(vararg seededSettings: CreditCardSettings) {
        settings.clear()
        settings.addAll(seededSettings)
        nextId = (seededSettings.maxOfOrNull { item -> item.id } ?: 0L) + 1L
        emitSettings()
    }

    private fun emitSettings() {
        settingsFlow.value = settings
            .sortedByDescending { item -> item.createdAt }
    }
}
