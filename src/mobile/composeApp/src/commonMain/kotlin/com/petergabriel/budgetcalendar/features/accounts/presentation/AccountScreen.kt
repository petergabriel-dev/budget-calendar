package com.petergabriel.budgetcalendar.features.accounts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.presentation.components.AccountFormSheet
import com.petergabriel.budgetcalendar.features.accounts.presentation.components.AccountListScreen
import com.petergabriel.budgetcalendar.features.creditcard.presentation.CreditCardViewModel
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardDetailSheet
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardPaymentSheet
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardSettingsForm
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinViewModel(),
    creditCardViewModel: CreditCardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val creditCardUiState by creditCardViewModel.uiState.collectAsStateWithLifecycle()
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreditCardSettingsForm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }

    LaunchedEffect(creditCardUiState.error) {
        val error = creditCardUiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        creditCardViewModel.clearError()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.spacing4, vertical = spacing.spacing4),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing4),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Text(
                        text = "Accounts",
                        style = typography.headline.copy(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            lineHeight = 38.sp,
                        ),
                        color = colors.textPrimary,
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.bgSurface)
                            .clickable(onClick = viewModel::showCreateForm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add account",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                AccountListScreen(
                    accounts = uiState.accounts.filter { account -> account.type != AccountType.CREDIT_CARD },
                    creditCards = creditCardUiState.creditCards,
                    balances = uiState.balances,
                    netWorth = uiState.netWorth,
                    modifier = Modifier.weight(1f),
                    onAccountTap = viewModel::showEditForm,
                    onCreditCardTap = { summary ->
                        showCreditCardSettingsForm = false
                        creditCardViewModel.selectCard(summary.accountId)
                    },
                )
            }

            if (uiState.isLoading || creditCardUiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            AccountFormSheet(
                isVisible = uiState.showForm,
                initialData = uiState.editingAccount,
                isSubmitting = uiState.isLoading,
                onSave = { name, type, balance, isInSpendingPool, description ->
                    val editing = uiState.editingAccount
                    if (editing == null) {
                        viewModel.createAccount(
                            CreateAccountRequest(
                                name = name,
                                type = type,
                                initialBalance = balance,
                                isInSpendingPool = isInSpendingPool,
                                description = description,
                            ),
                        )
                    } else {
                        viewModel.updateAccount(
                            id = editing.id,
                            request = UpdateAccountRequest(
                                name = name,
                                type = type,
                                balance = balance,
                                isInSpendingPool = isInSpendingPool,
                                description = description,
                            ),
                        )
                    }
                },
                onCancel = viewModel::hideForm,
                onDelete = { account -> viewModel.deleteAccount(account.id) },
            )

            creditCardUiState.selectedCard?.let { selectedCard ->
                CreditCardDetailSheet(
                    isVisible = !creditCardUiState.isPaymentSheetVisible && !showCreditCardSettingsForm,
                    summary = selectedCard,
                    settings = creditCardUiState.selectedCardSettings,
                    pendingTransactions = creditCardUiState.selectedCardTransactions,
                    onMakePayment = creditCardViewModel::openPaymentSheet,
                    onEditSettings = { showCreditCardSettingsForm = true },
                    onDismiss = {
                        showCreditCardSettingsForm = false
                        creditCardViewModel.dismissSelectedCard()
                    },
                )

                CreditCardPaymentSheet(
                    isVisible = creditCardUiState.isPaymentSheetVisible,
                    ccSummary = selectedCard,
                    suggestedAmount = creditCardUiState.suggestedPaymentAmount,
                    spendingPoolAccounts = creditCardUiState.spendingPoolAccounts,
                    onConfirmPayment = creditCardViewModel::makePayment,
                    onCancel = creditCardViewModel::closePaymentSheet,
                )

                CreditCardSettingsForm(
                    isVisible = showCreditCardSettingsForm,
                    initialSettings = creditCardUiState.selectedCardSettings,
                    onSave = { request ->
                        showCreditCardSettingsForm = false
                        creditCardViewModel.updateSettings(request)
                    },
                    onCancel = { showCreditCardSettingsForm = false },
                )
            }
        }
    }
}
