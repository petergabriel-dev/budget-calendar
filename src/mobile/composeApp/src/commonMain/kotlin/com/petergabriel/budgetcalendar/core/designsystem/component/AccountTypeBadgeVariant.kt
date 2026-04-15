package com.petergabriel.budgetcalendar.core.designsystem.component

import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType

// Stub: Badge variant extension
fun AccountType.toBadgeVariant(): BadgeVariant = when (this) {
    AccountType.CHECKING -> BadgeVariant.Info
    AccountType.SAVINGS -> BadgeVariant.Success
    AccountType.CREDIT_CARD -> BadgeVariant.Warning
    AccountType.CASH -> BadgeVariant.Pending
    AccountType.INVESTMENT -> BadgeVariant.Info
}
