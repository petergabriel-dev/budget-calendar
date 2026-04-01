package com.petergabriel.budgetcalendar.features.accounts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
fun SpendingPoolCard(
    account: Account,
    balance: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = BudgetCalendarTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = account.type.name,
                style = typography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
                color = Color.Black,
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
                color = Color.White,
            )
            Text(
                text = account.description.orEmpty(),
                style = typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = Color(0xFFA1A1AA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
