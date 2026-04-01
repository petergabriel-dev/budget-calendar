package com.petergabriel.budgetcalendar.features.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.model.Account
import com.petergabriel.budgetcalendar.features.accounts.domain.model.CreateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.model.UpdateAccountRequest
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CalculateNetWorthUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.CreateAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.DeleteAccountUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.UpdateAccountUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val createAccountUseCase: CreateAccountUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(isLoading = true))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private var accountsJob: Job? = null

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        accountsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }

        accountsJob = viewModelScope.launch {
            getAccountsUseCase()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { accounts ->
                    _uiState.update { state ->
                        state.copy(
                            accounts = accounts,
                            balances = accounts.associate { account -> account.id to account.balance },
                            editingAccount = resolveEditingAccount(state.editingAccount, accounts),
                            isLoading = false,
                            error = null,
                        )
                    }
                    refreshNetWorth()
                }
        }
    }

    fun createAccount(request: CreateAccountRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            createAccountUseCase(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showForm = false,
                            editingAccount = null,
                        )
                    }
                    refreshNetWorth()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
        }
    }

    fun updateAccount(id: Long, request: UpdateAccountRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            updateAccountUseCase(id, request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showForm = false,
                            editingAccount = null,
                        )
                    }
                    refreshNetWorth()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            deleteAccountUseCase(id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showForm = false,
                            editingAccount = null,
                        )
                    }
                    refreshNetWorth()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
        }
    }

    fun showCreateForm() {
        _uiState.update {
            it.copy(
                showForm = true,
                editingAccount = null,
            )
        }
    }

    fun showEditForm(account: Account) {
        _uiState.update {
            it.copy(
                showForm = true,
                editingAccount = account,
            )
        }
    }

    fun hideForm() {
        _uiState.update {
            it.copy(
                showForm = false,
                editingAccount = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun refreshNetWorth() {
        viewModelScope.launch {
            calculateNetWorthUseCase()
                .onSuccess { value ->
                    _uiState.update { it.copy(netWorth = value) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.message) }
                }
        }
    }

    private fun resolveEditingAccount(editingAccount: Account?, accounts: List<Account>): Account? {
        val editingId = editingAccount?.id ?: return null
        return accounts.firstOrNull { account -> account.id == editingId }
    }
}
