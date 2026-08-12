package com.petergabriel.budgetcalendar.features.accounts.domain.model

enum class AccountType(
    val dbValue: String,
    val isInSpendingPool: Boolean,
) {
    CHECKING("checking", true),
    SAVINGS("savings", false),
    CREDIT_CARD("credit_card", false),
    CASH("cash", true),
    INVESTMENT("investment", false);

    companion object {
        fun fromDbValue(value: String): AccountType {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: CHECKING
        }
    }
}

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val balance: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val description: String? = null,
) {
    val isInSpendingPool: Boolean
        get() = type.isInSpendingPool
}
