---
name: usecase-kmp
description: Use case patterns for business logic in Kotlin Multiplatform Clean Architecture
---

# Skill: UseCase KMP

## Project Context

- **Framework/Language**: Kotlin 1.9.22, Kotlin Multiplatform
- **Architecture**: Clean Architecture + MVVM
- **Project**: Budget Calendar

## Conventions

### Folder Structure

```
src/
├── features/
│   ├── accounts/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Account.kt
│   │   │   │   └── AccountType.kt
│   │   │   ├── repository/
│   │   │   │   └── IAccountRepository.kt
│   │   │   └── usecase/
│   │   │       ├── GetAccountsUseCase.kt
│   │   │       ├── GetAccountByIdUseCase.kt
│   │   │       ├── CreateAccountUseCase.kt
│   │   │       ├── UpdateAccountUseCase.kt
│   │   │       └── DeleteAccountUseCase.kt
│   ├── transactions/
│   │   └── domain/
│   │       └── usecase/
│   ├── calendar/
│   │   └── domain/
│   │       └── usecase/
│   └── sandbox/
│       └── domain/
│           └── usecase/
```

### Naming Conventions

- **Use Cases**: Verb + Entity + `UseCase` (e.g., `GetAccountsUseCase`, `CalculateBudgetUseCase`)
- **Use Case Result**: PascalCase + `Result` (e.g., `AccountResult`, `TransactionResult`)
- **Parameters**: Data classes with descriptive names (e.g., `CreateAccountParams`)

### Use Case Patterns

## Simple Use Cases

```kotlin
// src/features/accounts/domain/usecase/GetAccountsUseCase.kt
package com.budgetcalendar.features.accounts.domain.usecase

import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(
    private val repository: IAccountRepository
) {
    operator fun invoke(): Flow<List<Account>> {
        return repository.getAllAccounts()
    }

    fun getActive(): Flow<List<Account>> {
        return repository.getActiveAccounts()
    }

    fun getById(id: String): Flow<Account?> {
        return repository.getAccountById(id)
    }

    fun getByType(type: String): Flow<List<Account>> {
        return repository.getAccountsByType(type)
    }
}
```

## Parameter-Based Use Cases

```kotlin
// src/features/accounts/domain/usecase/CreateAccountUseCase.kt
package com.budgetcalendar.features.accounts.domain.usecase

import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.model.AccountType
import com.budgetcalendar.features.accounts.domain.repository.IAccountRepository

data class CreateAccountParams(
    val name: String,
    val type: AccountType,
    val balance: Long = 0L,
    val colorHex: String? = null,
    val iconName: String? = null
)

class CreateAccountUseCase(
    private val repository: IAccountRepository
) {
    suspend operator fun invoke(params: CreateAccountParams): Account {
        require(params.name.isNotBlank()) { "Account name cannot be empty" }
        require(params.balance >= 0) { "Initial balance cannot be negative" }

        val now = System.currentTimeMillis()
        val account = Account(
            id = java.util.UUID.randomUUID().toString(),
            name = params.name.trim(),
            type = params.type,
            balance = params.balance,
            colorHex = params.colorHex,
            iconName = params.iconName,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        repository.insertAccount(account)
        return account
    }
}
```

## Use Cases with Validation

```kotlin
// src/features/transactions/domain/usecase/CreateTransactionUseCase.kt
package com.budgetcalendar.features.transactions.domain.usecase

import com.budgetcalendar.features.transactions.domain.model.Transaction
import com.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.budgetcalendar.features.transactions.domain.repository.ITransactionRepository

data class CreateTransactionParams(
    val accountId: String,
    val amount: Long, // Positive for income, negative for expense
    val description: String,
    val category: String?,
    val date: Long,
    val status: TransactionStatus = TransactionStatus.PENDING
)

class CreateTransactionUseCase(
    private val transactionRepository: ITransactionRepository,
    private val accountRepository: com.budgetcalendar.features.accounts.domain.repository.IAccountRepository
) {
    suspend operator fun invoke(params: CreateTransactionParams): Transaction {
        // Validation
        require(params.accountId.isNotBlank()) { "Account ID is required" }
        require(params.amount != 0L) { "Transaction amount cannot be zero" }
        require(params.date > 0) { "Transaction date is required" }

        val now = System.currentTimeMillis()
        val transaction = Transaction(
            id = java.util.UUID.randomUUID().toString(),
            accountId = params.accountId,
            amount = params.amount,
            description = params.description.trim().ifBlank { null },
            category = params.category,
            date = params.date,
            status = params.status,
            createdAt = now,
            updatedAt = now
        )

        transactionRepository.insertTransaction(transaction)

        // Update account balance
        // Note: In a real app, this might be handled by a separate use case or domain event
        // For now, we'll let the account balance be calculated on read

        return transaction
    }
}
```

## Complex Business Logic Use Cases

```kotlin
// src/features/calendar/domain/usecase/GetMonthlyTransactionsUseCase.kt
package com.budgetcalendar.features.calendar.domain.usecase

import com.budgetcalendar.features.calendar.domain.model.CalendarDay
import com.budgetcalendar.features.calendar.domain.model.DayType
import com.budgetcalendar.features.transactions.domain.model.Transaction
import com.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class MonthlyTransactionsResult(
    val days: List<CalendarDay>,
    val totalIncome: Long,
    val totalExpense: Long,
    val netChange: Long
)

class GetMonthlyTransactionsUseCase(
    private val transactionRepository: ITransactionRepository
) {
    operator fun invoke(year: Int, month: Int): Flow<List<CalendarDay>> {
        val (startOfMonth, endOfMonth) = getMonthBounds(year, month)
        
        return transactionRepository.getTransactionsBetweenDates(startOfMonth, endOfMonth)
            .map { transactions -> buildCalendarDays(year, month, transactions) }
    }

    private fun getMonthBounds(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar months are 0-indexed
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val startOfMonth = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis
        
        return startOfMonth to endOfMonth
    }

    private fun buildCalendarDays(
        year: Int,
        month: Int,
        transactions: List<Transaction>
    ): List<CalendarDay> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

        val transactionsByDay = transactions.groupBy {
            val txCalendar = Calendar.getInstance().apply {
                timeInMillis = it.date
            }
            txCalendar.get(Calendar.DAY_OF_MONTH)
        }

        val calendarDays = mutableListOf<CalendarDay>()

        // Add empty days for the first week
        repeat(firstDayOfWeek) {
            calendarDays.add(
                CalendarDay(
                    dayOfMonth = 0,
                    dayType = DayType.EMPTY,
                    transactions = emptyList()
                )
            )
        }

        // Add actual days
        for (day in 1..daysInMonth) {
            val dayTransactions = transactionsByDay[day] ?: emptyList()
            val hasTransactions = dayTransactions.isNotEmpty()
            val hasPending = dayTransactions.any { it.status == TransactionStatus.PENDING }
            val hasOverdue = dayTransactions.any { 
                it.status == TransactionStatus.PENDING && it.date < System.currentTimeMillis() 
            }

            calendarDays.add(
                CalendarDay(
                    dayOfMonth = day,
                    dayType = when {
                        hasOverdue -> DayType.HAS_OVERDUE
                        hasPending -> DayType.HAS_PENDING
                        hasTransactions -> DayType.HAS_TRANSACTIONS
                        else -> DayType.EMPTY
                    },
                    transactions = dayTransactions,
                    totalAmount = dayTransactions.sumOf { it.amount }
                )
            )
        }

        return calendarDays
    }
}
```

## Budget Calculation Use Cases

```kotlin
// src/features/sandbox/domain/usecase/CalculateBudgetUseCase.kt
package com.budgetcalendar.features.sandbox.domain.usecase

import com.budgetcalendar.features.accounts.domain.model.Account
import com.budgetcalendar.features.accounts.domain.repository.IAccountRepository
import com.budgetcalendar.features.transactions.domain.model.Transaction
import com.budgetcalendar.features.transactions.domain.model.TransactionStatus
import com.budgetcalendar.features.transactions.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BudgetSummary(
    val totalBalance: Long,
    val availableToBudget: Long,
    val pendingTransactions: Long,
    val confirmedThisMonth: Long,
    val rolloverAmount: Long,
    val categories: List<CategorySpending>
)

data class CategorySpending(
    val category: String,
    val budgeted: Long,
    val spent: Long,
    val remaining: Long
)

class CalculateBudgetUseCase(
    private val accountRepository: IAccountRepository,
    private val transactionRepository: ITransactionRepository
) {
    operator fun invoke(monthStart: Long, monthEnd: Long): Flow<BudgetSummary> {
        return combine(
            accountRepository.getActiveAccounts(),
            transactionRepository.getTransactionsBetweenDates(monthStart, monthEnd)
        ) { accounts, transactions ->
            calculateBudget(accounts, transactions, monthStart)
        }
    }

    private fun calculateBudget(
        accounts: List<Account>,
        transactions: List<Transaction>,
        monthStart: Long
    ): BudgetSummary {
        val totalBalance = accounts.sumOf { it.balance }
        
        val confirmedTransactions = transactions.filter { 
            it.status == TransactionStatus.CONFIRMED 
        }
        
        val pendingTransactions = transactions.filter { 
            it.status == TransactionStatus.PENDING 
        }
        
        val confirmedThisMonth = confirmedTransactions.sumOf { it.amount }
        
        val pendingAmount = pendingTransactions.sumOf { it.amount }
        
        // Safe to spend = total balance - pending transactions
        val availableToBudget = totalBalance - pendingAmount
        
        // Calculate rollover from previous month
        val rolloverAmount = calculateRollover(accounts, monthStart)
        
        // Group by category
        val categories = confirmedTransactions
            .filter { it.amount < 0 } // Expenses only
            .groupBy { it.category ?: "Uncategorized" }
            .map { (category, txs) ->
                val spent = -txs.sumOf { it.amount } // Convert to positive
                CategorySpending(
                    category = category,
                    budgeted = 0, // Would come from budget settings
                    spent = spent,
                    remaining = 0 - spent // Would use budgeted - spent
                )
            }

        return BudgetSummary(
            totalBalance = totalBalance,
            availableToBudget = availableToBudget,
            pendingTransactions = pendingAmount,
            confirmedThisMonth = confirmedThisMonth,
            rolloverAmount = rolloverAmount,
            categories = categories
        )
    }

    private fun calculateRollover(accounts: List<Account>, monthStart: Long): Long {
        // Rollover = sum of positive transaction amounts from previous months
        // that weren't budgeted
        // This is a simplified version - real implementation would track budget allocations
        return 0L
    }
}
```

## Sandbox Mode Use Cases

```kotlin
// src/features/sandbox/domain/usecase/SimulateTransactionUseCase.kt
package com.budgetcalendar.features.sandbox.domain.usecase

import com.budgetcalendar.features.transactions.domain.model.Transaction
import com.budgetcalendar.features.transactions.domain.model.TransactionStatus

data class SimulationResult(
    val originalBalance: Long,
    val simulatedBalance: Long,
    val impact: Long,
    val wouldCauseOverdraft: Boolean,
    val accountNewBalance: Map<String, Long>
)

data class SimulateTransactionParams(
    val accountId: String,
    val amount: Long,
    val description: String,
    val category: String?,
    val date: Long
)

class SimulateTransactionUseCase(
    private val accountRepository: com.budgetcalendar.features.accounts.domain.repository.IAccountRepository
) {
    suspend operator fun invoke(
        params: SimulateTransactionParams,
        currentAccounts: List<Account>
    ): SimulationResult {
        val account = currentAccounts.find { it.id == params.accountId }
            ?: throw IllegalArgumentException("Account not found")

        val originalBalance = account.balance
        val simulatedBalance = originalBalance + params.amount
        
        return SimulationResult(
            originalBalance = originalBalance,
            simulatedBalance = simulatedBalance,
            impact = params.amount,
            wouldCauseOverdraft = simulatedBalance < 0,
            accountNewBalance = mapOf(params.accountId to simulatedBalance)
        )
    }
}
```

## Restrictions

- **ALWAYS** use constructor injection for repositories in use cases
- **ALWAYS** validate inputs using `require()` or custom validation
- **ALWAYS** return domain models, never database entities
- **ALWAYS** use `suspend` functions for operations that modify data
- **ALWAYS** use `Flow` for reactive queries that return multiple items
- **NEVER** expose database queries or SQLDelight code in use cases
- **ALWAYS** name use cases with verbs: Get, Create, Update, Delete, Calculate, Simulate
- **NEVER** put business logic in ViewModels - delegate to use cases
- **ALWAYS** wrap complex parameters in data classes for clarity
- **ALWAYS** handle errors with meaningful exception messages
