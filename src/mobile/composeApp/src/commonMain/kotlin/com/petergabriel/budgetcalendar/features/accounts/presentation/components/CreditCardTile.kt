package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.creditcard.domain.model.CreditCardSummary
import kotlin.math.abs

@Composable
fun CreditCardTile(
    summary: CreditCardSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val owed = abs(summary.currentBalance)
    val availableCredit = summary.availableCredit
    val creditLimit = summary.creditLimit
    val utilization = if (creditLimit != null && creditLimit > 0L) {
        (owed.toFloat() / creditLimit.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    val statementDue = summary.statementDueAmount?.let { amount ->
        val dateText = summary.statementDueDate?.let { dueDate -> " due ${DateUtils.formatMonthDay(dueDate)}" }.orEmpty()
        "${amount.formatCurrency()}$dateText"
    }
    val secondary = buildString {
        append("Owed ${owed.formatCurrency()}")
        if (statementDue != null) append(" · $statementDue")
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF4F4F5))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.bgSurface)
                    .border(BorderStroke(1.5.dp, colors.borderStrong), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "💳 CREDIT CARD",
                    style = typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    ),
                    color = colors.textSecondary,
                )
            }

            Text(
                text = summary.accountName,
                style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF71717A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = availableCredit?.formatCurrency() ?: "Limit not set",
                style = typography.section.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 26.4.sp,
                    letterSpacing = 0.sp,
                ),
                color = Color.Black,
            )
            Text(
                text = if (creditLimit == null) secondary else "$secondary · of ${creditLimit.formatCurrency()}",
                style = typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = Color(0xFF71717A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (utilization != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { utilization },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color.Black,
                    trackColor = colors.borderSubtle,
                )
                Text(
                    text = "Utilization ${(utilization * 100).toInt()}%",
                    style = typography.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
