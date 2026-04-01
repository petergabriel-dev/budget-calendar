package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account

@Composable
fun WalledGardenCard(
    account: Account,
    balance: Long,
    isLiability: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography

    val badgeModifier = if (isLiability) {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSurface)
            .border(BorderStroke(1.5.dp, colors.borderStrong), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    } else {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF4F4F5))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = badgeModifier) {
            Text(
                text = if (isLiability) "LIABILITY" else account.type.name,
                style = typography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
                color = if (isLiability) colors.textSecondary else Color.White,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = balance.formatCurrency(),
                style = typography.section.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 26.4.sp,
                    letterSpacing = 0.sp,
                ),
                color = Color.Black,
            )
            Text(
                text = account.description ?: account.name,
                style = typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = Color(0xFF71717A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
