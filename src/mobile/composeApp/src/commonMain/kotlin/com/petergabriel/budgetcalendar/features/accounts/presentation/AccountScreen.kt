package com.petergabriel.budgetcalendar.features.accounts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcHeader
import com.petergabriel.budgetcalendar.core.designsystem.component.BcNotificationBell
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.AccountType
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.presentation.components.AccountDetailsSheet
import com.petergabriel.budgetcalendar.features.accounts.presentation.components.AccountFormSheet
import com.petergabriel.budgetcalendar.features.accounts.presentation.components.AccountListScreen
import com.petergabriel.budgetcalendar.features.creditcard.presentation.CreditCardEffect
import com.petergabriel.budgetcalendar.features.creditcard.presentation.CreditCardViewModel
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardDetailSheet
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardPaymentSheet
import com.petergabriel.budgetcalendar.features.creditcard.presentation.components.CreditCardSettingsForm
import kotlinx.coroutines.flow.collect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onBellClick: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
    creditCardViewModel: CreditCardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val creditCardUiState by creditCardViewModel.uiState.collectAsStateWithLifecycle()
    val spacing = BudgetCalendarTheme.spacing
    var showCreditCardSettingsForm by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AccountEffect.DeleteSucceeded -> {
                    if (selectedAccount?.id == effect.accountId) {
                        selectedAccount = null
                    }
                    if (creditCardViewModel.uiState.value.selectedCard?.accountId == effect.accountId) {
                        showCreditCardSettingsForm = false
                        creditCardViewModel.dismissSelectedCard()
                    }
                }
            }
        }
    }

    LaunchedEffect(creditCardViewModel) {
        creditCardViewModel.effects.collect { effect ->
            when (effect) {
                CreditCardEffect.SettingsUpdated -> showCreditCardSettingsForm = false
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                BcHeader(
                    title = "Accounts",
                    modifier = Modifier.fillMaxWidth(),
                    actionContent = {
                        BcNotificationBell(
                            unreadCount = unreadCount,
                            onClick = onBellClick,
                        )
                    },
                )

                AccountListScreen(
                    accounts = uiState.accounts.filter { account -> account.type != AccountType.CREDIT_CARD },
                    creditCards = creditCardUiState.creditCards,
                    balances = uiState.balances,
                    netWorth = uiState.netWorth,
                    isNetWorthLoading = uiState.isNetWorthLoading,
                    netWorthError = uiState.netWorthError,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.error,
                    isCreditCardsLoading = creditCardUiState.isLoading,
                    creditCardsError = creditCardUiState.creditCardsError,
                    modifier = Modifier.weight(1f),
                    onRetryAccounts = viewModel::loadAccounts,
                    onRetryCreditCards = creditCardViewModel::retryCreditCards,
                    onAddAccount = viewModel::showCreateForm,
                    onAccountTap = { account -> selectedAccount = account },
                    onCreditCardTap = { summary ->
                        showCreditCardSettingsForm = false
                        creditCardViewModel.selectCard(summary.accountId)
                    },
                )
            }

            AccountFormSheet(
                isVisible = uiState.showForm,
                initialData = uiState.editingAccount,
                isSubmitting = uiState.isSubmitting,
                errorMessage = uiState.mutationError,
                onSave = { name, type, balance, description, creditLimit, statementClosingDay, paymentDueDay ->
                    val editing = uiState.editingAccount
                    if (editing == null) {
                        viewModel.createAccount(
                            CreateAccountRequest(
                                name = name,
                                type = type,
                                initialBalance = if (type == AccountType.CREDIT_CARD) -balance else balance,
                                description = description,
                                creditLimit = creditLimit,
                                statementClosingDay = statementClosingDay,
                                paymentDueDay = paymentDueDay,
                            ),
                        )
                    } else {
                        viewModel.updateAccount(
                            id = editing.id,
                            request = UpdateAccountRequest(
                                name = name,
                                type = type,
                                description = description,
                            ),
                        )
                    }
                },
                onCancel = viewModel::hideForm,
            )

            selectedAccount?.let { account ->
                AccountDetailsSheet(
                    isVisible = !uiState.showForm,
                    account = account,
                    isDeleting = uiState.isDeleting,
                    errorMessage = uiState.mutationError,
                    onEdit = {
                        selectedAccount = null
                        viewModel.showEditForm(account)
                    },
                    onDelete = { viewModel.deleteAccount(account.id) },
                    onDismiss = { if (!uiState.isDeleting) selectedAccount = null },
                )
            }

            creditCardUiState.selectedCard?.let { selectedCard ->
                CreditCardDetailSheet(
                    isVisible = !creditCardUiState.isPaymentSheetVisible && !showCreditCardSettingsForm,
                    summary = selectedCard,
                    settings = creditCardUiState.selectedCardSettings,
                    pendingTransactions = creditCardUiState.selectedCardTransactions,
                    onMakePayment = creditCardViewModel::openPaymentSheet,
                    onEditSettings = { showCreditCardSettingsForm = true },
                    onDismiss = creditCardViewModel::dismissSelectedCard,
                )

                CreditCardPaymentSheet(
                    isVisible = creditCardUiState.isPaymentSheetVisible,
                    ccSummary = selectedCard,
                    suggestedAmount = creditCardUiState.suggestedPaymentAmount,
                    paymentSourceAccounts = creditCardUiState.paymentSourceAccounts,
                    isSubmitting = creditCardUiState.isPaymentSubmitting,
                    errorMessage = creditCardUiState.paymentError,
                    onConfirmPayment = creditCardViewModel::makePayment,
                    onCancel = creditCardViewModel::closePaymentSheet,
                )

                CreditCardSettingsForm(
                    isVisible = showCreditCardSettingsForm,
                    initialSettings = creditCardUiState.selectedCardSettings,
                    isSubmitting = creditCardUiState.isSettingsSubmitting || uiState.isDeleting,
                    errorMessage = creditCardUiState.settingsError ?: uiState.mutationError,
                    onSave = creditCardViewModel::updateSettings,
                    onCancel = { if (!creditCardUiState.isSettingsSubmitting && !uiState.isDeleting) showCreditCardSettingsForm = false },
                    onDelete = { viewModel.deleteAccount(selectedCard.accountId) },
                )
            }
        }
    }
}
