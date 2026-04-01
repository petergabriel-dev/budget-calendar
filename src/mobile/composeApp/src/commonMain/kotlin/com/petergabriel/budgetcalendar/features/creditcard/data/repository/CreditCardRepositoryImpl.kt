package com.petergabriel.budgetcalendar.features.creditcard.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.petergabriel.budgetcalendar.core.database.BudgetCalendarDatabase
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.creditcard.data.local.CreditCardSettingsEntity
import com.petergabriel.budgetcalendar.features.creditcard.data.mapper.CreditCardMapper
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSettings
import com.petergabriel.budgetcalendar.features.creditcard.domain.repository.ICreditCardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CreditCardRepositoryImpl(
    private val database: BudgetCalendarDatabase,
    private val creditCardMapper: CreditCardMapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ICreditCardRepository {

    override fun getAllSettings(): Flow<List<CreditCardSettings>> {
        return database.credit_card_settingsQueries
            .getAllCreditCardSettings(::toSettingsEntity)
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map(creditCardMapper::toDomain) }
    }

    override suspend fun getSettingsByAccountId(accountId: Long): Result<CreditCardSettings> = runCatching {
        database.credit_card_settingsQueries
            .getCreditCardSettingsByAccountId(accountId, ::toSettingsEntity)
            .executeAsOneOrNull()
            ?.let(creditCardMapper::toDomain)
            ?: throw NoSuchElementException("Credit card settings for accountId=$accountId were not found")
    }

    override suspend fun insert(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
    ): Result<CreditCardSettings> = runCatching {
        val now = DateUtils.nowMillis()
        database.credit_card_settingsQueries.insertCreditCardSettings(
            accountId,
            creditLimit,
            statementBalance,
            dueDate,
            now,
            now,
        )
        getSettingsByAccountId(accountId).getOrThrow()
    }

    override suspend fun update(
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
    ): Result<CreditCardSettings> = runCatching {
        database.credit_card_settingsQueries.updateCreditCardSettings(
            creditLimit,
            statementBalance,
            dueDate,
            DateUtils.nowMillis(),
            accountId,
        )
        getSettingsByAccountId(accountId).getOrThrow()
    }

    override suspend fun delete(accountId: Long): Result<Unit> = runCatching {
        database.credit_card_settingsQueries.deleteCreditCardSettings(accountId)
    }

    override suspend fun getReservedAmount(accountId: Long): Result<Long> = runCatching {
        database.credit_card_settingsQueries.getCreditCardReservedAmount(accountId).executeAsOne()
    }

    override suspend fun getAllReservedAmounts(): Result<Map<Long, Long>> = runCatching {
        database.credit_card_settingsQueries
            .getAllCreditCardReservedAmounts(creditCardMapper::toReservedAmountRow)
            .executeAsList()
            .associate { row -> row.accountId to row.reservedAmount }
    }

    private fun toSettingsEntity(
        id: Long,
        accountId: Long,
        creditLimit: Long?,
        statementBalance: Long?,
        dueDate: Long?,
        createdAt: Long,
        updatedAt: Long,
    ): CreditCardSettingsEntity {
        return CreditCardSettingsEntity(
            id = id,
            accountId = accountId,
            creditLimit = creditLimit,
            statementBalance = statementBalance,
            dueDate = dueDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
