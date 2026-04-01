package com.petergabriel.budgetcalendar.features.budget.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcSectionHeader
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.budget.presentation.components.BudgetSummaryCard
import com.petergabriel.budgetcalendar.features.budget.presentation.components.CreditCardReservationItem
import com.petergabriel.budgetcalendar.features.budget.presentation.components.SafeToSpendHeader
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = koinViewModel(),
) {
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }

    Scaffold(
        modifier = modifier,
        containerColor = colors.bgPrimary,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.spacing4),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
                contentPadding = PaddingValues(
                    top = spacing.spacing4,
                    bottom = 88.dp,
                ),
            ) {
                item {
                    SafeToSpendHeader(
                        amount = uiState.budgetSummary.availableToSpend,
                        lastUpdated = uiState.budgetSummary.lastCalculatedAt,
                        isCalculating = uiState.isCalculating,
                        onRefresh = viewModel::refresh,
                    )
                }

                item {
                    BudgetSummaryCard(
                        summary = uiState.budgetSummary,
                        onDetailsTap = viewModel::loadRolloverHistory,
                    )
                }

                if (uiState.spendingPoolAccounts.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(radius.xl),
                        ) {
                            Text(
                                text = "No spending pool accounts yet. Add one in Accounts to start tracking Safe to Spend.",
                                style = typography.bodyMedium,
                                color = colors.textSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.spacing3),
                            )
                        }
                    }
                } else {
                    item {
                        BcSectionHeader(
                            title = "Credit Card Reservations",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (uiState.creditCardReservations.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(radius.xl),
                            ) {
                                Text(
                                    text = "No reserved credit card payments.",
                                    style = typography.bodyMedium,
                                    color = colors.textSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(spacing.spacing3),
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.creditCardReservations,
                            key = { reservation -> reservation.accountId },
                        ) { reservation ->
                            CreditCardReservationItem(
                                reservation = reservation,
                                onPaymentTap = { viewModel.refresh() },
                            )
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.size(1.dp))
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.spacing4, vertical = 2.dp),
                )
            }
        }
    }
}
