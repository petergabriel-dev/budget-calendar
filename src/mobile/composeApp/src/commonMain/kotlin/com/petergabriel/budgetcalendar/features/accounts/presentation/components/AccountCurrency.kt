package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

internal fun Long.formatCurrency(): String = CurrencyUtils.formatCents(this)
