package com.petergabriel.budgetcalendar.features.accounts.domain.model

enum class AccountType(val dbValue: String) {
    CHECKING("checking"),
    SAVINGS("savings"),
    CREDIT_CARD("credit_card"),
    CASH("cash"),
    INVESTMENT("investment");

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
    val isInSpendingPool: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val description: String? = null,
)
