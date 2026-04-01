package com.petergabriel.budgetcalendar.features.budget.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.features.budget.domain.model.CreditCardReservation

@Composable
fun CreditCardReservationItem(
    reservation: CreditCardReservation,
    onPaymentTap: (CreditCardReservation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasReservation = reservation.reservedAmount > 0L

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = reservation.accountName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                Button(
                    onClick = { onPaymentTap(reservation) },
                    enabled = hasReservation,
                ) {
                    Text("Pay Now")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (hasReservation) {
                        "${CurrencyUtils.formatCents(reservation.reservedAmount)} reserved"
                    } else {
                        "$0.00 reserved"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hasReservation) MaterialTheme.colorScheme.onSecondaryContainer else Color(0xFF90A4AE),
                    modifier = Modifier
                        .background(
                            color = if (hasReservation) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
