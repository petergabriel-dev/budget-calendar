---
name: calendar-feature-kmp
description: Calendar view implementation patterns for Kotlin Multiplatform (Jetpack Compose & SwiftUI)
---

# Skill: Calendar Feature KMP

## Project Context

- **Framework/Language**: Kotlin 1.9.22, Kotlin Multiplatform
- **Platforms**: Android (Jetpack Compose), iOS (SwiftUI)
- **Feature**: Calendar View with 7-column month grid
- **Project**: Budget Calendar

## Conventions

### Folder Structure

```
src/
├── features/
│   ├── calendar/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── CalendarDay.kt
│   │   │   │   ├── DayType.kt
│   │   │   │   └── CalendarMonth.kt
│   │   │   ├── usecase/
│   │   │   │   ├── GetMonthlyTransactionsUseCase.kt
│   │   │   │   └── GetDayTransactionsUseCase.kt
│   │   │   └── repository/
│   │   │       └── ICalendarRepository.kt
│   │   ├── data/
│   │   │   └── repository/
│   │   │       └── CalendarRepositoryImpl.kt
│   │   └── presentation/
│   │       ├── android/
│   │       │   ├── viewmodel/
│   │       │   │   └── CalendarViewModel.kt
│   │       │   └── screen/
│   │       │       └── CalendarScreen.kt
│   │       └── ios/
│   │           ├── viewmodel/
│   │           │   └── CalendarViewModel.swift
│   │           └── screen/
│   │               └── CalendarView.swift
```

### Naming Conventions

- **Calendar Models**: PascalCase (e.g., `CalendarDay`, `CalendarMonth`)
- **Day Types**: Enum values describing day state (e.g., `EMPTY`, `HAS_TRANSACTIONS`, `HAS_PENDING`)
- **View Models**: Feature + `ViewModel` (e.g., `CalendarViewModel`)

## Domain Layer

```kotlin
// src/features/calendar/domain/model/CalendarDay.kt
package com.budgetcalendar.features.calendar.domain.model

import com.budgetcalendar.features.transactions.domain.model.Transaction

data class CalendarDay(
    val dayOfMonth: Int,
    val dayType: DayType,
    val transactions: List<Transaction>,
    val totalAmount: Long = 0L,
    val isToday: Boolean = false,
    val isSelected: Boolean = false
) {
    val hasTransactions: Boolean get() = transactions.isNotEmpty()
    val isExpenseDay: Boolean get() = transactions.any { it.amount < 0 }
    val isIncomeDay: Boolean get() = transactions.any { it.amount > 0 }
}
```

```kotlin
// src/features/calendar/domain/model/DayType.kt
package com.budgetcalendar.features.calendar.domain.model

enum class DayType {
    EMPTY,              // Day outside current month
    TODAY,              // Current day
    HAS_TRANSACTIONS,   // Has confirmed transactions
    HAS_PENDING,        // Has pending transactions
    HAS_OVERDUE,        // Has overdue pending transactions
    SELECTED            // Currently selected day
}
```

```kotlin
// src/features/calendar/domain/model/CalendarMonth.kt
package com.budgetcalendar.features.calendar.domain.model

data class CalendarMonth(
    val year: Int,
    val month: Int, // 1-12
    val days: List<CalendarDay>,
    val monthName: String,
    val totalIncome: Long,
    val totalExpense: Long,
    val netChange: Long
) {
    val daysInMonth: Int get() = days.count { it.dayOfMonth > 0 }
    val firstDayOfWeek: Int get() = days.indexOfFirst { it.dayOfMonth == 1 }
}
```

## Android Implementation (Jetpack Compose)

```kotlin
// src/features/calendar/presentation/android/viewmodel/CalendarViewModel.kt
package com.budgetcalendar.features.calendar.presentation.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetcalendar.features.calendar.domain.model.CalendarDay
import com.budgetcalendar.features.calendar.domain.model.CalendarMonth
import com.budgetcalendar.features.calendar.domain.usecase.GetMonthlyTransactionsUseCase
import com.budgetcalendar.features.transactions.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalendarState(
    val currentMonth: CalendarMonth? = null,
    val selectedDay: CalendarDay? = null,
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTransactionDetail: Boolean = false
)

sealed class CalendarEvent {
    data class SelectDay(val day: CalendarDay) : CalendarEvent()
    data object NextMonth : CalendarEvent()
    data object PreviousMonth : CalendarEvent()
    data object GoToToday : CalendarEvent()
    data class AddTransaction(val date: Long) : CalendarEvent()
    data class ViewTransaction(val transaction: Transaction) : CalendarEvent()
}

class CalendarViewModel(
    private val getMonthlyTransactionsUseCase: GetMonthlyTransactionsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val calendar = Calendar.getInstance()

    init {
        loadCurrentMonth()
    }

    fun handleEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.SelectDay -> selectDay(event.day)
            is CalendarEvent.NextMonth -> navigateMonth(1)
            is CalendarEvent.PreviousMonth -> navigateMonth(-1)
            is CalendarEvent.GoToToday -> goToToday()
            is CalendarEvent.AddTransaction -> openAddTransaction(event.date)
            is CalendarEvent.ViewTransaction -> viewTransaction(event.transaction)
        }
    }

    private fun loadCurrentMonth() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        loadMonth(year, month)
    }

    private fun loadMonth(year: Int, month: Int) {
        _state.update { it.copy(isLoading = true, error = null) }

        getMonthlyTransactionsUseCase(year, month)
            .onEach { days ->
                val monthName = getMonthName(month)
                val totalIncome = days.sumOf { d -> d.transactions.filter { it.amount > 0 }.sumOf { it.amount } }
                val totalExpense = days.sumOf { d -> d.transactions.filter { it.amount < 0 }.sumOf { it.amount } }

                val currentMonth = CalendarMonth(
                    year = year,
                    month = month,
                    days = days,
                    monthName = monthName,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netChange = totalIncome + totalExpense
                )

                _state.update {
                    it.copy(
                        currentMonth = currentMonth,
                        isLoading = false
                    )
                }
            }
            .catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load calendar"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun selectDay(day: CalendarDay) {
        if (day.dayOfMonth <= 0) return

        _state.update {
            it.copy(
                selectedDay = day,
                selectedDayTransactions = day.transactions,
                showTransactionDetail = day.hasTransactions
            )
        }
    }

    private fun navigateMonth(delta: Int) {
        calendar.add(Calendar.MONTH, delta)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        loadMonth(year, month)

        // Clear selection when navigating
        _state.update { it.copy(selectedDay = null) }
    }

    private fun goToToday() {
        calendar.timeInMillis = System.currentTimeMillis()
        loadCurrentMonth()
        _state.update { it.copy(selectedDay = null, showTransactionDetail = false) }
    }

    private fun openAddTransaction(date: Long) {
        // Navigate to add transaction screen
    }

    private fun viewTransaction(transaction: Transaction) {
        // Navigate to transaction detail
    }

    private fun getMonthName(month: Int): String {
        return listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )[month - 1]
    }
}
```

```kotlin
// src/features/calendar/presentation/android/screen/CalendarScreen.kt
package com.budgetcalendar.features.calendar.presentation.android.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budgetcalendar.features.calendar.domain.model.CalendarDay
import com.budgetcalendar.features.calendar.domain.model.DayType
import com.budgetcalendar.features.calendar.presentation.android.viewmodel.CalendarEvent
import com.budgetcalendar.features.calendar.presentation.android.viewmodel.CalendarViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CalendarTopBar(
                monthName = state.currentMonth?.monthName ?: "",
                year = state.currentMonth?.year ?: 0,
                onPrevious = { viewModel.handleEvent(CalendarEvent.PreviousMonth) },
                onNext = { viewModel.handleEvent(CalendarEvent.NextMonth) },
                onToday = { viewModel.handleEvent(CalendarEvent.GoToToday) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Card
            state.currentMonth?.let { month ->
                BudgetSummaryCard(
                    income = month.totalIncome,
                    expense = month.totalExpense,
                    net = month.netChange
                )
            }

            // Day of Week Headers
            DayOfWeekHeader()

            // Calendar Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(state.currentMonth?.days ?: emptyList()) { day ->
                    CalendarDayCell(
                        day = day,
                        isSelected = day == state.selectedDay,
                        onClick = { viewModel.handleEvent(CalendarEvent.SelectDay(day)) }
                    )
                }
            }
        }

        // Bottom Sheet for Selected Day
        if (state.showTransactionDetail && state.selectedDay != null) {
            BottomSheet(
                onDismissRequest = { viewModel.handleEvent(CalendarEvent.SelectDay(state.selectedDay!!)) }
            ) {
                DayTransactionsSheet(
                    day = state.selectedDay!!,
                    transactions = state.selectedDayTransactions
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    monthName: String,
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Month")
                }
                Text(
                    text = "$monthName $year",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Month")
                }
            }
        },
        actions = {
            TextButton(onClick = onToday) {
                Text("Today")
            }
        }
    )
}

@Composable
private fun DayOfWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.dayType == DayType.TODAY -> MaterialTheme.colorScheme.primaryContainer
        day.dayType == DayType.HAS_OVERDUE -> MaterialTheme.colorScheme.errorContainer
        day.dayType == DayType.HAS_PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        day.dayType == DayType.HAS_TRANSACTIONS -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        day.dayOfMonth <= 0 -> Color.Transparent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = day.dayOfMonth > 0) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (day.dayOfMonth > 0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                // Transaction indicator dot
                if (day.hasTransactions) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    income: Long,
    expense: Long,
    net: Long
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Income", style = MaterialTheme.typography.labelSmall)
                Text(
                    currencyFormat.format(income / 100.0),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF4CAF50)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Expense", style = MaterialTheme.typography.labelSmall)
                Text(
                    currencyFormat.format(expense / 100.0),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF44336)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Net", style = MaterialTheme.typography.labelSmall)
                Text(
                    currencyFormat.format(net / 100.0),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (net >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun DayTransactionsSheet(
    day: CalendarDay,
    transactions: List<com.budgetcalendar.features.transactions.domain.model.Transaction>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Transactions for Day ${day.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Text("No transactions", style = MaterialTheme.typography.bodyMedium)
        } else {
            transactions.forEach { tx ->
                TransactionRow(transaction = tx)
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: com.budgetcalendar.features.transactions.domain.model.Transaction
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(transaction.description ?: "No description")
        Text(
            currencyFormat.format(transaction.amount / 100.0),
            color = if (transaction.amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}
```

## iOS Implementation (SwiftUI)

```swift
// src/features/calendar/presentation/ios/viewmodel/CalendarViewModel.swift
import Foundation
import Combine
import SwiftUI

@MainActor
class CalendarViewModel: ObservableObject {
    @Published var currentMonth: CalendarMonth?
    @Published var selectedDay: CalendarDay?
    @Published var selectedDayTransactions: [Transaction] = []
    @Published var isLoading: Bool = false
    @Published var error: String?
    @Published var showTransactionDetail: Bool = false
    
    private let getMonthlyTransactionsUseCase: GetMonthlyTransactionsUseCase
    private var cancellables = Set<AnyCancellable>()
    private let calendar = Calendar.current
    
    init(getMonthlyTransactionsUseCase: GetMonthlyTransactionsUseCase) {
        self.getMonthlyTransactionsUseCase = getMonthlyTransactionsUseCase
        loadCurrentMonth()
    }
    
    func loadCurrentMonth() {
        let now = Date()
        let year = calendar.component(.year, from: now)
        let month = calendar.component(.month, from: now)
        loadMonth(year: year, month: month)
    }
    
    func loadMonth(year: Int, month: Int) {
        isLoading = true
        error = nil
        
        getMonthlyTransactionsUseCase(year: year, month: month)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let failure) = completion {
                        self?.error = failure.localizedDescription
                    }
                },
                receiveValue: { [weak self] days in
                    guard let self = self else { return }
                    let monthName = self.getMonthName(month: month)
                    let totalIncome = days.filter { $0.amount > 0 }.reduce(0) { $0 + $1.amount }
                    let totalExpense = days.filter { $0.amount < 0 }.reduce(0) { $0 + $1.amount }
                    
                    self.currentMonth = CalendarMonth(
                        year: year,
                        month: month,
                        days: days,
                        monthName: monthName,
                        totalIncome: totalIncome,
                        totalExpense: totalExpense,
                        netChange: totalIncome + totalExpense
                    )
                }
            )
            .store(in: &cancellables)
    }
    
    func selectDay(_ day: CalendarDay) {
        guard day.dayOfMonth > 0 else { return }
        
        selectedDay = day
        selectedDayTransactions = day.transactions
        showTransactionDetail = day.hasTransactions
    }
    
    func nextMonth() {
        guard var month = currentMonth?.month, var year = currentMonth?.year else { return }
        if month == 12 {
            month = 1
            year += 1
        } else {
            month += 1
        }
        loadMonth(year: year, month: month)
        selectedDay = nil
    }
    
    func previousMonth() {
        guard var month = currentMonth?.month, var year = currentMonth?.year else { return }
        if month == 1 {
            month = 12
            year -= 1
        } else {
            month -= 1
        }
        loadMonth(year: year, month: month)
        selectedDay = nil
    }
    
    func goToToday() {
        loadCurrentMonth()
        selectedDay = nil
        showTransactionDetail = false
    }
    
    private func getMonthName(month: Int) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM"
        var components = DateComponents()
        components.month = month
        if let date = calendar.date(from: components) {
            return formatter.string(from: date)
        }
        return ""
    }
}
```

```swift
// src/features/calendar/presentation/ios/screen/CalendarView.swift
import SwiftUI

struct CalendarView: View {
    @StateObject private var viewModel: CalendarViewModel
    
    init(viewModel: CalendarViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Month Navigation
                HStack {
                    Button { viewModel.previousMonth() } label: {
                        Image(systemName: "chevron.left")
                    }
                    Spacer()
                    Text("\(viewModel.currentMonth?.monthName ?? "") \(viewModel.currentMonth?.year ?? 0)")
                        .font(.headline)
                    Spacer()
                    Button { viewModel.nextMonth() } label: {
                        Image(systemName: "chevron.right")
                    }
                }
                .padding()
                
                // Summary Card
                if let month = viewModel.currentMonth {
                    BudgetSummaryCard(
                        income: month.totalIncome,
                        expense: month.totalExpense,
                        net: month.netChange
                    )
                }
                
                // Day Headers
                HStack {
                    ForEach(["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"], id: \.self) { day in
                        Text(day)
                            .frame(maxWidth: .infinity)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                
                // Calendar Grid
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: 8) {
                    ForEach(viewModel.currentMonth?.days ?? [], id: \.dayOfMonth) { day in
                        CalendarDayCell(
                            day: day,
                            isSelected: day == viewModel.selectedDay,
                            onTap: { viewModel.selectDay(day) }
                        )
                    }
                }
                .padding(.horizontal)
                
                Spacer()
            }
            .navigationTitle("Calendar")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("Today") {
                        viewModel.goToToday()
                    }
                }
            }
            .sheet(isPresented: $viewModel.showTransactionDetail) {
                if let day = viewModel.selectedDay {
                    DayTransactionsSheet(day: day, transactions: viewModel.selectedDayTransactions)
                }
            }
        }
    }
}

struct CalendarDayCell: View {
    let day: CalendarDay
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                if day.dayOfMonth > 0 {
                    Text("\(day.dayOfMonth)")
                        .font(.body)
                        .foregroundColor(isSelected ? .white : .primary)
                    
                    if day.hasTransactions {
                        Circle()
                            .fill(isSelected ? .white : .blue)
                            .frame(width: 6, height: 6)
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .background(
                Circle()
                    .fill(backgroundColor)
            )
        }
        .disabled(day.dayOfMonth <= 0)
    }
    
    private var backgroundColor: Color {
        if isSelected { return .blue }
        switch day.dayType {
        case .today: return .blue.opacity(0.2)
        case .hasOverdue: return .red.opacity(0.2)
        case .hasPending: return .orange.opacity(0.2)
        case .hasTransactions: return .green.opacity(0.2)
        default: return .clear
        }
    }
}

struct BudgetSummaryCard: View {
    let income: Int64
    let expense: Int64
    let net: Int64
    
    var body: some View {
        HStack {
            VStack {
                Text("Income")
                    .font(.caption)
                Text(formatCurrency(income))
                    .foregroundColor(.green)
            }
            Spacer()
            VStack {
                Text("Expense")
                    .font(.caption)
                Text(formatCurrency(expense))
                    .foregroundColor(.red)
            }
            Spacer()
            VStack {
                Text("Net")
                    .font(.caption)
                Text(formatCurrency(net))
                    .foregroundColor(net >= 0 ? .green : .red)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding()
    }
    
    private func formatCurrency(_ amount: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "USD"
        return formatter.string(from: NSNumber(value: Double(amount) / 100)) ?? "$0.00"
    }
}

struct DayTransactionsSheet: View {
    let day: CalendarDay
    let transactions: [Transaction]
    
    var body: some View {
        NavigationStack {
            List(transactions) { transaction in
                HStack {
                    Text(transaction.description ?? "No description")
                    Spacer()
                    Text(formatCurrency(transaction.amount))
                        .foregroundColor(transaction.amount >= 0 ? .green : .red)
                }
            }
            .navigationTitle("Day \(day.dayOfMonth) Transactions")
        }
    }
    
    private func formatCurrency(_ amount: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "USD"
        return formatter.string(from: NSNumber(value: Double(amount) / 100)) ?? "$0.00"
    }
}

#Preview {
    // Preview implementation
}
```

## Restrictions

- **ALWAYS** use 7-column grid for month view (Sun-Sat or Mon-Sun)
- **ALWAYS** display empty cells for days outside the current month
- **ALWAYS** highlight today with distinct visual indicator
- **ALWAYS** show transaction count or indicator dots for days with transactions
- **ALWAYS** distinguish between pending, confirmed, and overdue transactions visually
- **ALWAYS** handle month navigation (next/previous)
- **ALWAYS** provide "Go to Today" navigation option
- **ALWAYS** show monthly summary (income, expense, net) in the calendar view
- **ALWAYS** use platform-native date formatting for month/year headers
- **NEVER** load all transactions at once - use month-based pagination
