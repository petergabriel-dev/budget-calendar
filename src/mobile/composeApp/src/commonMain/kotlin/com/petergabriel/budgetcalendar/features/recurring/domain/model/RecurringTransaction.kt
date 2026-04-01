package com.petergabriel.budgetcalendar.features.recurring.domain.model

enum class RecurrenceType(val dbValue: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer");

    companion object {
        fun fromDbValue(value: String): RecurrenceType {
            return entries.firstOrNull { type ->
                type.dbValue.equals(value, ignoreCase = true)
            } ?: EXPENSE
        }
    }
}

data class RecurringTransaction(
    val id: Long,
    val accountId: Long,
    val destinationAccountId: Long?,
    val amount: Long,
    val dayOfMonth: Int,
    val type: RecurrenceType,
    val description: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
