package com.petergabriel.budgetcalendar.features.transactions.domain.model

data class CreateTransactionRequest(
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val amount: Long,
    val date: Long,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val description: String? = null,
    val category: String? = null,
    val linkedTransactionId: Long? = null,
    val signedAmount: Long = defaultSignedAmount(type, amount, linkedTransactionId),
    val isSandbox: Boolean = false,
) {
    companion object {
        private fun defaultSignedAmount(
            type: TransactionType,
            amount: Long,
            linkedTransactionId: Long?,
        ): Long {
            return when (type) {
                TransactionType.INCOME -> amount
                TransactionType.EXPENSE -> -amount
                TransactionType.TRANSFER -> if (linkedTransactionId == null) -amount else amount
            }
        }
    }
}
