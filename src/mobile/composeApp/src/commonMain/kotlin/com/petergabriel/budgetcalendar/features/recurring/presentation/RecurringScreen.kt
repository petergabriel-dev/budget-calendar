package com.petergabriel.budgetcalendar.features.recurring.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.features.recurring.domain.model.UpdateRecurringTransactionRequest
import com.petergabriel.budgetcalendar.features.recurring.presentation.components.RecurringFormSheet
import com.petergabriel.budgetcalendar.features.recurring.presentation.components.RecurringListScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecurringScreen(
    modifier: Modifier = Modifier,
    viewModel: RecurringViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
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
            RecurringListScreen(
                recurringTransactions = uiState.recurringTransactions,
                upcomingGenerated = uiState.upcomingGenerated,
                onRecurringTap = viewModel::showEditForm,
                onToggleActive = { recurring, isActive ->
                    viewModel.toggleActive(recurring.id, isActive)
                },
                onAddRecurring = viewModel::showCreateForm,
                onGenerateMonthly = viewModel::generateMonthly,
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            RecurringFormSheet(
                isVisible = uiState.showForm,
                availableAccounts = uiState.availableAccounts,
                isSubmitting = uiState.isLoading,
                error = uiState.error,
                initialData = uiState.editingRecurring,
                onSave = { request ->
                    val editing = uiState.editingRecurring
                    if (editing == null) {
                        viewModel.createRecurring(request)
                    } else {
                        viewModel.updateRecurring(
                            id = editing.id,
                            request = UpdateRecurringTransactionRequest(
                                accountId = request.accountId,
                                destinationAccountId = request.destinationAccountId,
                                amount = request.amount,
                                dayOfMonth = request.dayOfMonth,
                                type = request.type,
                                description = request.description,
                                isActive = editing.isActive,
                            ),
                        )
                    }
                },
                onCancel = viewModel::hideForm,
                onDelete = { recurring -> viewModel.deleteRecurring(recurring.id) },
            )
        }
    }
}
