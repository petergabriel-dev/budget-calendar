package com.petergabriel.budgetcalendar.features.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.model.contains
import com.petergabriel.budgetcalendar.features.calendar.domain.model.plusMonths
import com.petergabriel.budgetcalendar.features.calendar.domain.model.toYearMonth
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.BuildCalendarMonthUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.CalculateMonthProjectionUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.usecase.GetMonthTransactionsUseCase
import com.petergabriel.budgetcalendar.features.transactions.domain.model.Transaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class CalendarViewModel(
    private val getMonthTransactionsUseCase: GetMonthTransactionsUseCase,
    private val buildCalendarMonthUseCase: BuildCalendarMonthUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val calculateMonthProjectionUseCase: CalculateMonthProjectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var monthTransactionsJob: Job? = null
    private var monthProjectionJob: Job? = null
    private var accountNamesJob: Job? = null

    init {
        observeAccounts()
        loadMonth(_uiState.value.currentMonth)
    }

    fun loadMonth(yearMonth: YearMonth) {
        monthTransactionsJob?.cancel()
        monthProjectionJob?.cancel()

        val placeholderMonth = buildCalendarMonthUseCase(
            yearMonth = yearMonth,
            transactions = emptyList(),
        )
        _uiState.update { state ->
            state.copy(
                currentMonth = yearMonth,
                calendarMonth = placeholderMonth,
                isMonthLoading = true,
                monthError = null,
                isProjectionLoading = true,
                projectionError = null,
            )
        }

        monthTransactionsJob = viewModelScope.launch {
            getMonthTransactionsUseCase(yearMonth)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isMonthLoading = false,
                            monthError = throwable.message ?: "Unable to load month",
                        )
                    }
                }
                .collect { monthTransactions ->
                    publishMonthState(
                        yearMonth = yearMonth,
                        monthTransactions = monthTransactions,
                    )
                }
        }

        observeProjection(yearMonth)
    }

    fun retryProjection() {
        _uiState.update { state ->
            state.copy(
                isProjectionLoading = true,
                projectionError = null,
            )
        }
        observeProjection(_uiState.value.currentMonth)
    }

    fun showMonthContaining(date: LocalDate) {
        if (!_uiState.value.currentMonth.contains(date)) {
            loadMonth(date.toYearMonth())
        }
    }

    fun navigateMonth(direction: Int) {
        if (direction == 0) {
            return
        }

        val targetMonth = _uiState.value.currentMonth.plusMonths(direction)
        loadMonth(targetMonth)
    }

    override fun onCleared() {
        monthTransactionsJob?.cancel()
        monthProjectionJob?.cancel()
        accountNamesJob?.cancel()
        super.onCleared()
    }

    private fun observeAccounts() {
        accountNamesJob?.cancel()
        accountNamesJob = viewModelScope.launch {
            getAccountsUseCase()
                .catch { }
                .collect { accounts ->
                    _uiState.update { state ->
                        state.copy(
                            accountNamesById = accounts.associate { account -> account.id to account.name },
                        )
                    }
                }
        }
    }

    private fun observeProjection(yearMonth: YearMonth) {
        monthProjectionJob?.cancel()
        monthProjectionJob = viewModelScope.launch {
            calculateMonthProjectionUseCase(yearMonth)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isProjectionLoading = false,
                            projectionError = throwable.message ?: "Unable to load projection",
                        )
                    }
                }
                .collect { projection ->
                    _uiState.update { state ->
                        state.copy(
                            endOfMonthProjection = projection,
                            isProjectionLoading = false,
                            projectionError = null,
                        )
                    }
                }
        }
    }

    private fun publishMonthState(
        yearMonth: YearMonth,
        monthTransactions: List<Transaction>,
    ) {
        val calendarMonth = buildCalendarMonthUseCase(
            yearMonth = yearMonth,
            transactions = monthTransactions,
        )

        _uiState.update { state ->
            state.copy(
                currentMonth = yearMonth,
                calendarMonth = calendarMonth,
                isMonthLoading = false,
                monthError = null,
            )
        }
    }
}
