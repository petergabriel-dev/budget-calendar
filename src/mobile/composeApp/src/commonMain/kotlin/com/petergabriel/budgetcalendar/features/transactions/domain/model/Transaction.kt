package com.petergabriel.budgetcalendar.features.transactions.domain.model

enum class TransactionType(val dbValue: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer");

    companion object {
        fun fromDbValue(value: String): TransactionType {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: EXPENSE
        }
    }
}

enum class TransactionStatus(val dbValue: String) {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    OVERDUE("overdue"),
    CANCELLED("cancelled");

    companion object {
        fun fromDbValue(value: String): TransactionStatus {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

data class Transaction(
    val id: Long,
    val accountId: Long,
    val destinationAccountId: Long?,
    val amount: Long,
    val date: Long,
    val type: TransactionType,
    val status: TransactionStatus,
    val description: String?,
    val category: String?,
    val linkedTransactionId: Long?,
    val isSandbox: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
