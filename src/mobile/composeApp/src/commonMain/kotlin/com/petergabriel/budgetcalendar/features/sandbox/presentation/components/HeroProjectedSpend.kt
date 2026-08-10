package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHero
import com.petergabriel.budgetcalendar.core.designsystem.component.BcMetricHeroTone
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils

@Composable
fun HeroProjectedSpend(
    projectedSafeToSpend: Long,
    dailyRate: Long? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BcMetricHero(
        label = "PROJECTED SPEND",
        amount = projectedSafeToSpend,
        tone = if (projectedSafeToSpend <= 0L) BcMetricHeroTone.Negative else BcMetricHeroTone.Neutral,
        supportingText = dailyRate?.let { rate -> "${CurrencyUtils.formatCents(rate)} / day" },
        warningMessage = if (!isLoading && errorMessage.isNullOrBlank() && projectedSafeToSpend <= 0L) {
            "Projected spend is zero or negative"
        } else {
            null
        },
        isLoading = isLoading,
        errorMessage = errorMessage,
        onRetry = onRetry,
        modifier = modifier,
    )
}
