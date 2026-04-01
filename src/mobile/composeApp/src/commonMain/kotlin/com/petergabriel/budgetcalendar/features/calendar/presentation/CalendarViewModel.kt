package com.petergabriel.budgetcalendar.features.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petergabriel.budgetcalendar.features.accounts.domain.usecase.GetAccountsUseCase
import com.petergabriel.budgetcalendar.features.calendar.domain.model.contains
import com.petergabriel.budgetcalendar.features.calendar.domain.model.firstDay
import com.petergabriel.budgetcalendar.features.calendar.domain.model.lengthInDays
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
import kotlinx.datetime.onDay
import kotlin.math.min

class CalendarViewModel(
    private val getMonthTransactionsUseCase: GetMonthTransactionsUseCase,
    private val buildCalendarMonthUseCase: BuildCalendarMonthUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val calculateMonthProjectionUseCase: CalculateMonthProjectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var monthTransactionsJob: Job? = null
    private var monthProjectionJob: Job? = null
    private var accountNamesJob: Job? = null
    private var cachedTransactions: List<Transaction> = emptyList()

    init {
        observeAccounts()
        loadMonth(_uiState.value.currentMonth)
    }

    fun loadMonth(yearMonth: YearMonth) {
        monthTransactionsJob?.cancel()
        monthProjectionJob?.cancel()
        _uiState.update { state ->
            state.copy(
                currentMonth = yearMonth,
                endOfMonthProjection = 0L,
                isLoading = true,
                error = null,
            )
        }

        monthTransactionsJob = viewModelScope.launch {
            getMonthTransactionsUseCase(yearMonth)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = throwable.message,
                        )
                    }
                }
                .collect { monthTransactions ->
                    cachedTransactions = monthTransactions
                    val resolvedSelectedDate = resolveSelectedDate(yearMonth, _uiState.value.selectedDate)
                    publishMonthState(
                        yearMonth = yearMonth,
                        selectedDate = resolvedSelectedDate,
                        monthTransactions = monthTransactions,
                    )
                }
        }

        monthProjectionJob = viewModelScope.launch {
            calculateMonthProjectionUseCase(yearMonth)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(error = throwable.message)
                    }
                }
                .collect { projection ->
                    _uiState.update { state ->
                        state.copy(endOfMonthProjection = projection)
                    }
                }
        }
    }

    fun selectDate(date: LocalDate) {
        val currentMonth = _uiState.value.currentMonth
        if (!currentMonth.contains(date)) {
            _uiState.update { state -> state.copy(selectedDate = date) }
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
        monthProjectionJob?.cancel()
        accountNamesJob?.cancel()
        super.onCleared()
    }

    private fun observeAccounts() {
        accountNamesJob?.cancel()
        accountNamesJob = viewModelScope.launch {
            getAccountsUseCase()
                .catch { throwable ->
                    _uiState.update { state -> state.copy(error = throwable.message) }
                }
                .collect { accounts ->
                    _uiState.update { state ->
                        state.copy(
                            accountNamesById = accounts.associate { account -> account.id to account.name },
                        )
                    }
                }
        }
    }

    private fun resolveSelectedDate(targetMonth: YearMonth, selectedDate: LocalDate): LocalDate {
        if (targetMonth.contains(selectedDate)) {
            return selectedDate
        }

        val day = min(selectedDate.day, targetMonth.lengthInDays())
        return targetMonth.onDay(day)
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
            state.copy(
                currentMonth = yearMonth,
                selectedDate = selectedDate,
                calendarMonth = calendarMonth,
                selectedDayTransactions = calendarMonth.transactionsByDate[selectedDate].orEmpty(),
                isLoading = false,
                error = null,
            )
        }
    }
}
