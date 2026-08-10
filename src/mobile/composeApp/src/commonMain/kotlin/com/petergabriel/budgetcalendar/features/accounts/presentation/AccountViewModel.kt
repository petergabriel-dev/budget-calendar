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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _effects = MutableSharedFlow<AccountEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AccountEffect> = _effects.asSharedFlow()

    private var accountsJob: Job? = null

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        accountsJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isNetWorthLoading = true,
                netWorthError = null,
            )
        }

        accountsJob = viewModelScope.launch {
            getAccountsUseCase()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNetWorthLoading = false,
                            error = throwable.message ?: "Unable to load accounts",
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
        if (_uiState.value.isSubmitting || _uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, mutationError = null) }

            createAccountUseCase(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showForm = false,
                            editingAccount = null,
                            mutationError = null,
                        )
                    }
                    refreshNetWorth()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            mutationError = throwable.message ?: "Unable to save account",
                        )
                    }
                }
        }
    }

    fun updateAccount(id: Long, request: UpdateAccountRequest) {
        if (_uiState.value.isSubmitting || _uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, mutationError = null) }

            updateAccountUseCase(id, request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showForm = false,
                            editingAccount = null,
                            mutationError = null,
                        )
                    }
                    refreshNetWorth()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            mutationError = throwable.message ?: "Unable to save account",
                        )
                    }
                }
        }
    }

    fun deleteAccount(id: Long) {
        if (_uiState.value.isSubmitting || _uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, mutationError = null) }

            deleteAccountUseCase(id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showForm = false,
                            editingAccount = null,
                            mutationError = null,
                        )
                    }
                    _effects.tryEmit(AccountEffect.DeleteSucceeded(id))
                    refreshNetWorth()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            mutationError = throwable.message ?: "Unable to delete account",
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
                mutationError = null,
            )
        }
    }

    fun showEditForm(account: Account) {
        _uiState.update {
            it.copy(
                showForm = true,
                editingAccount = account,
                mutationError = null,
            )
        }
    }

    fun hideForm() {
        _uiState.update {
            it.copy(
                showForm = false,
                editingAccount = null,
                mutationError = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, mutationError = null) }
    }

    private fun refreshNetWorth() {
        _uiState.update { it.copy(isNetWorthLoading = true, netWorthError = null) }
        viewModelScope.launch {
            calculateNetWorthUseCase()
                .onSuccess { value ->
                    _uiState.update {
                        it.copy(
                            netWorth = value,
                            isNetWorthLoading = false,
                            netWorthError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isNetWorthLoading = false,
                            netWorthError = throwable.message ?: "Unable to load net worth",
                        )
                    }
                }
        }
    }

    private fun resolveEditingAccount(editingAccount: Account?, accounts: List<Account>): Account? {
        val editingId = editingAccount?.id ?: return null
        return accounts.firstOrNull { account -> account.id == editingId }
    }
}
