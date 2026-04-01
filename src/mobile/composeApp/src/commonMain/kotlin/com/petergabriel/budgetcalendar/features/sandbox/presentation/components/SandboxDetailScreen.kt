package com.petergabriel.budgetcalendar.features.sandbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petergabriel.budgetcalendar.core.designsystem.component.BcButton
import com.petergabriel.budgetcalendar.core.designsystem.component.ButtonVariant
import com.petergabriel.budgetcalendar.core.designsystem.theme.BudgetCalendarTheme
import com.petergabriel.budgetcalendar.core.utils.CurrencyUtils
import com.petergabriel.budgetcalendar.core.utils.DateUtils
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxComparison
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxSnapshot
import com.petergabriel.budgetcalendar.features.sandbox.domain.model.SandboxTransaction
import com.petergabriel.budgetcalendar.features.transactions.domain.model.CreateTransactionRequest
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.petergabriel.budgetcalendar.features.transactions.domain.model.TransactionType
import com.petergabriel.budgetcalendar.features.transactions.presentation.TransactionFormViewModel
import com.petergabriel.budgetcalendar.features.transactions.presentation.components.TransactionFormSheet
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SandboxDetailScreen(
    snapshot: SandboxSnapshot,
    sandboxTransactions: List<SandboxTransaction>,
    sandboxSafeToSpend: Long,
    comparison: SandboxComparison?,
    isComparing: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onToggleComparison: () -> Unit,
    onAddSimulation: (CreateTransactionRequest) -> Unit,
    onRemoveSimulation: (SandboxTransaction) -> Unit,
    onPromoteTransaction: (SandboxTransaction) -> Unit,
    transactionFormViewModel: TransactionFormViewModel = koinViewModel(),
) {
    val transactionFormUiState by transactionFormViewModel.uiState.collectAsStateWithLifecycle()
    var showTransactionForm by remember { mutableStateOf(false) }
    val initialDateMillis = remember { DateUtils.startOfDayMillis(DateUtils.nowMillis()) }
    val spacing = BudgetCalendarTheme.spacing
    val colors = BudgetCalendarTheme.colors

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            BcButton(
                text = "Add",
                onClick = {
                    transactionFormViewModel.setType(TransactionType.EXPENSE)
                    showTransactionForm = true
                },
                variant = ButtonVariant.PrimaryIconLeading,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.spacing4, vertical = spacing.spacing3),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Header(
                snapshot = snapshot,
                safeToSpend = sandboxSafeToSpend,
                onBack = onBack,
                onToggleComparison = onToggleComparison,
                isComparing = isComparing,
            )

            if (isComparing && comparison != null) {
                ComparisonDisplay(
                    realSafeToSpend = comparison.realSafeToSpend,
                    sandboxSafeToSpend = comparison.sandboxSafeToSpend,
                    difference = comparison.difference,
                )
            }

            if (sandboxTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No simulation transactions yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sandboxTransactions, key = { transaction -> transaction.id }) { transaction ->
                        SandboxTransactionItem(
                            transaction = transaction,
                            onPromote = onPromoteTransaction,
                            onRemove = onRemoveSimulation,
                        )
                    }
                }
            }
        }
    }

    TransactionFormSheet(
        isVisible = showTransactionForm,
        uiState = transactionFormUiState,
        initialDateMillis = initialDateMillis,
        onSetType = transactionFormViewModel::setType,
        onSave = { request ->
            onAddSimulation(
                request.copy(
                    status = TransactionStatus.PENDING,
                    isSandbox = true,
                ),
            )
            showTransactionForm = false
        },
        onCancel = {
            transactionFormViewModel.clearError()
            showTransactionForm = false
        },
    )
}

@Composable
private fun Header(
    snapshot: SandboxSnapshot,
    safeToSpend: Long,
    onBack: () -> Unit,
    onToggleComparison: () -> Unit,
    isComparing: Boolean,
) {
    val colors = BudgetCalendarTheme.colors
    val spacing = BudgetCalendarTheme.spacing
    val typography = BudgetCalendarTheme.typography
    val radius = BudgetCalendarTheme.radius

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.bgSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(radius.xl),
            )
            .padding(spacing.spacing3),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("Back")
            }

            TextButton(onClick = onToggleComparison) {
                Text(if (isComparing) "Hide Compare" else "Compare")
            }
        }

        Text(
            text = snapshot.name,
            style = typography.section,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )

        Text(
            text = "Sandbox Safe to Spend",
            style = typography.bodySmall,
            color = colors.textSecondary,
        )

        Text(
            text = CurrencyUtils.formatCents(safeToSpend),
            style = typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
    }
}
