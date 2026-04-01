package com.petergabriel.budgetcalendar.features.transactions.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BadgeVariant
import com.petergabriel.budgetcalendar.core.designsystem.component.BcBadge
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus

@Composable
fun TransactionStatusBadge(
    status: TransactionStatus,
    modifier: Modifier = Modifier,
) {
    val (label, variant) = status.toBadge()
    BcBadge(
        text = label,
        variant = variant,
        modifier = modifier,
    )
}

private fun TransactionStatus.toBadge(): Pair<String, BadgeVariant> {
    return when (this) {
        TransactionStatus.PENDING -> "Pending" to BadgeVariant.Pending
        TransactionStatus.OVERDUE -> "Overdue" to BadgeVariant.Overdue
        TransactionStatus.CONFIRMED -> "Confirmed" to BadgeVariant.Success
        TransactionStatus.CANCELLED -> "Cancelled" to BadgeVariant.Info
    }
}
