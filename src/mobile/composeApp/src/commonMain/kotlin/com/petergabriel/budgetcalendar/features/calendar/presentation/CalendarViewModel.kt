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
    private var selectedDayTransactionsJob: Job? = null
    private var monthProjectionJob: Job? = null
    private var accountNamesJob: Job? = null
    private var cachedTransactions: List<Transaction> = emptyList()

    init {
        observeAccounts()
        loadMonth(_uiState.value.currentMonth)
    }

    fun loadMonth(yearMonth: YearMonth) {
        monthTransactionsJob?.cancel()
        selectedDayTransactionsJob?.cancel()
        monthProjectionJob?.cancel()
        cachedTransactions = emptyList()

        val selectedDate = _uiState.value.selectedDate
        val placeholderMonth = buildCalendarMonthUseCase(
            yearMonth = yearMonth,
            transactions = emptyList(),
            selectedDate = selectedDate,
        )
        _uiState.update { state ->
            state.copy(
                currentMonth = yearMonth,
                calendarMonth = placeholderMonth,
                selectedDayTransactions = emptyList(),
                isMonthLoading = true,
                monthError = null,
                isProjectionLoading = true,
                projectionError = null,
                isSelectedDayLoading = true,
                selectedDayError = null,
            )
        }

        monthTransactionsJob = viewModelScope.launch {
            getMonthTransactionsUseCase(yearMonth)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isMonthLoading = false,
                            monthError = throwable.message ?: "Unable to load month",
                            isSelectedDayLoading = if (yearMonth.contains(state.selectedDate)) false else state.isSelectedDayLoading,
                        )
                    }
                }
                .collect { monthTransactions ->
                    cachedTransactions = monthTransactions
                    publishMonthState(
                        yearMonth = yearMonth,
                        selectedDate = _uiState.value.selectedDate,
                        monthTransactions = monthTransactions,
                    )
                }
        }

        if (!yearMonth.contains(selectedDate)) {
            selectedDayTransactionsJob = viewModelScope.launch {
                getMonthTransactionsUseCase(selectedDate.toYearMonth())
                    .catch { throwable ->
                        _uiState.update { state ->
                            state.copy(
                                isSelectedDayLoading = false,
                                selectedDayError = throwable.message ?: "Unable to load selected day",
                            )
                        }
                    }
                    .collect { selectedDayTransactions ->
                        val calendarMonth = buildCalendarMonthUseCase(
                            yearMonth = selectedDate.toYearMonth(),
                            transactions = selectedDayTransactions,
                            selectedDate = selectedDate,
                        )
                        _uiState.update { state ->
                            state.copy(
                                selectedDayTransactions = calendarMonth.transactionsByDate[selectedDate].orEmpty(),
                                isSelectedDayLoading = false,
                                selectedDayError = null,
                            )
                        }
                    }
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

    fun selectDate(date: LocalDate) {
        val currentMonth = _uiState.value.currentMonth
        selectedDayTransactionsJob?.cancel()
        selectedDayTransactionsJob = null
        _uiState.update { state ->
            state.copy(
                selectedDate = date,
                selectedDayTransactions = emptyList(),
                isSelectedDayLoading = true,
                selectedDayError = null,
            )
        }

        if (!currentMonth.contains(date)) {
            loadMonth(date.toYearMonth())
            return
        }

        publishMonthState(
            yearMonth = currentMonth,
            selectedDate = date,
            monthTransactions = cachedTransactions,
        )
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
        selectedDayTransactionsJob?.cancel()
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
        selectedDate: LocalDate,
        monthTransactions: List<Transaction>,
    ) {
        val calendarMonth = buildCalendarMonthUseCase(
            yearMonth = yearMonth,
            transactions = monthTransactions,
            selectedDate = selectedDate,
        )

        _uiState.update { state ->
            val selectedDateIsInMonth = yearMonth.contains(selectedDate)
            state.copy(
                currentMonth = yearMonth,
                selectedDate = selectedDate,
                calendarMonth = calendarMonth,
                isMonthLoading = false,
                monthError = null,
                selectedDayTransactions = if (selectedDateIsInMonth) {
                    calendarMonth.transactionsByDate[selectedDate].orEmpty()
                } else {
                    state.selectedDayTransactions
                },
                isSelectedDayLoading = if (selectedDateIsInMonth) false else state.isSelectedDayLoading,
                selectedDayError = if (selectedDateIsInMonth) null else state.selectedDayError,
            )
        }
    }
}
