package com.petergabriel.budgetcalendar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils


enum class BcMetricHeroHierarchy {
    Primary,
    Secondary,
}

enum class BcMetricHeroTone {
    Neutral,
    Positive,
    Warning,
    Negative,
}

@Composable
fun BcMetricHero(
    label: String,
    amount: Long,
    hierarchy: BcMetricHeroHierarchy = BcMetricHeroHierarchy.Primary,
    tone: BcMetricHeroTone = BcMetricHeroTone.Neutral,
    supportingText: String? = null,
    warningMessage: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onWarningClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val spacing = BudgetCalendarTheme.spacing
    val radius = BudgetCalendarTheme.radius
    val amountColor = when (tone) {
        BcMetricHeroTone.Neutral -> colors.textPrimary
        BcMetricHeroTone.Positive -> colors.colorSuccessText
        BcMetricHeroTone.Warning -> colors.colorWarningText
        BcMetricHeroTone.Negative -> colors.colorErrorText
    }
    val supportingColor = when (tone) {
        BcMetricHeroTone.Warning -> colors.colorWarningText
        BcMetricHeroTone.Negative -> colors.colorErrorText
        BcMetricHeroTone.Positive -> colors.colorSuccessText
        BcMetricHeroTone.Neutral -> colors.textSecondary
    }
    val formattedAmount = CurrencyUtils.formatCents(amount)
    val toneDescription = when (tone) {
        BcMetricHeroTone.Neutral -> "neutral"
        BcMetricHeroTone.Positive -> "positive"
        BcMetricHeroTone.Warning -> "warning"
        BcMetricHeroTone.Negative -> "negative"
    }
    val amountDescription = "$label: $formattedAmount, $toneDescription"
    val amountStyle = when (hierarchy) {
        BcMetricHeroHierarchy.Primary -> typography.displayMedium
        BcMetricHeroHierarchy.Secondary -> typography.title
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
    ) {
        Text(
            text = label,
            modifier = Modifier.semantics { heading() },
            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary,
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spacing.spacing12)
                        .background(
                            color = colors.bgSurface,
                            shape = RoundedCornerShape(radius.md),
                        )
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                )
            }

            !errorMessage.isNullOrBlank() -> {
                Text(
                    text = errorMessage,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    style = typography.bodyMedium,
                    color = colors.colorErrorText,
                )
                onRetry?.let { retry ->
                    BcButton(
                        text = "Retry",
                        onClick = retry,
                        variant = ButtonVariant.Outline,
                    )
                }
            }

            else -> {
                Text(
                    text = formattedAmount,
                    modifier = Modifier.semantics {
                        contentDescription = amountDescription
                    },
                    style = amountStyle,
                    color = amountColor,
                )
                supportingText?.let { support ->
                    Text(
                        text = support,
                        style = typography.bodyMedium,
                        color = supportingColor,
                    )
                }
                warningMessage?.let { warning ->
                    val warningModifier = Modifier
                        .defaultMinSize(minHeight = spacing.spacing12)
                        .padding(vertical = spacing.spacing2)
                        .then(
                            if (onWarningClick == null) {
                                Modifier
                            } else {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClick = onWarningClick,
                                )
                            },
                        )
                    Text(
                        text = warning,
                        modifier = warningModifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                        style = typography.bodySmall,
                        color = colors.colorWarningText,
                    )
                }
            }
        }
    }
}
