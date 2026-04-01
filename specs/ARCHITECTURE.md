# Architecture

## Context

The Budget Calendar app follows **Clean Architecture** with **MVVM** pattern, built on **Kotlin Multiplatform** for cross-platform mobile development (iOS & Android). The application uses an **offline-first** approach with local SQLite database via SQLDelight, enabling full functionality without network dependency. Future backend integration will follow RESTful conventions for cloud sync capabilities.

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION LAYER                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  Android: Jetpack Compose          │  iOS: SwiftUI                          │
│  - ViewModels (Kotlin)             │  - ViewModels (Kotlin/Native)         │
│  - UI State Management             │  - UI State Management                │
│  - Navigation                      │  - Navigation                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               DOMAIN LAYER                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  - Use Cases / Interactors                                                 │
│  - Domain Models (Pure Kotlin, platform-agnostic)                         │
│  - Repository Interfaces (Abstractions)                                     │
│  - Business Rules & Validation                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                DATA LAYER                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  - SQLDelight (Local SQLite)              │  Remote Data Sources (Future)  │
│  - Repository Implementations             │  - REST API Client             │
│  - Data Models (DTOs)                      │  - Sync Services               │
│  - Mappers                                  │                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PLATFORM LAYER (Shared)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  - Koin (Dependency Injection)         │  Date/Calendar Utilities          │
│  - Coroutines / Flow                     │  Platform-Specific Extensions    │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Data Flow

### Write Operations (Transaction Creation/Modification)
```
UI (Compose/SwiftUI)
    │
    ▼
ViewModel.validate() ──► Domain Use Case.execute()
    │                           │
    │                           ▼
    │                    Repository Interface.save()
    │                           │
    ▼                           ▼
UI State Update ◄────── SQLDelight (Local DB)
```

### Read Operations (Calendar/Budget Display)
```
User Selects Date/Views Calendar
    │
    ▼
ViewModel.loadData() ──► Use Case.getTransactions()
    │                           │
    │                           ▼
    │                    Repository.getByDateRange()
    │                           │
    ▼                           ▼
UI State Flow ◄────── SQLDelight Queries
    │
    ▼
Compose/SwiftUI UI Recomposition
```

### Budget Calculation Flow
```
All Account Balances
    │
    ▼
CalculateSpendingPoolUseCase
    │ (Filter: accounts with isSpendingPool = true)
    ▼
ApplyPendingTransactions (Reserve Funds)
    │ (Deduct all PENDING and OVERDUE transactions)
    ▼
ApplyConfirmedTransactions (Real Deductions)
    │
    ▼
Real-Time "Safe to Spend" Balance
```

## Package by Feature Structure

```
src/
├── commonMain/
│   └── kotlin/
│       └── com/budgetcalendar/
│           ├── di/                      # Koin Dependency Injection
│           │   └── KoinModules.kt
│           │
│           ├── features/
│           │   ├── accounts/            # Account Management Feature
│           │   │   ├── data/
│           │   │   │   ├── local/       # SQLDelight queries
│           │   │   │   ├── repository/  # Repository impl
│           │   │   │   └── mapper/      # DTO ↔ Domain mappers
│           │   │   ├── domain/
│           │   │   │   ├── model/       # Account, AccountType
│           │   │   │   ├── repository/   # IAccountRepository interface
│           │   │   │   └── usecase/     # CreateAccount, GetAccounts...
│           │   │   └── presentation/
│           │   │       ├── AccountViewModel.kt
│           │   │       └── AccountUiState.kt
│           │   │
│           │   ├── transactions/        # Transaction Lifecycle Feature
│           │   │   ├── data/
│           │   │   │   ├── local/
│           │   │   │   ├── repository/
│           │   │   │   └── mapper/
│           │   │   ├── domain/
│           │   │   │   ├── model/       # Transaction, TransactionState
│           │   │   │   ├── repository/
│           │   │   │   └── usecase/     # CreateTransaction, ConfirmTransaction...
│           │   │   └── presentation/
│           │   │       ├── TransactionViewModel.kt
│           │   │       └── TransactionUiState.kt
│           │   │
│           │   ├── calendar/            # Calendar View Feature
│           │   │   ├── domain/
│           │   │   │   ├── model/       # CalendarDay, CalendarEvent
│           │   │   │   └── usecase/      # GetMonthTransactions, CalculateDailyBalance
│           │   │   └── presentation/
│           │   │       ├── CalendarViewModel.kt
│           │   │       └── CalendarUiState.kt
│           │   │
│           │   ├── sandbox/             # What-If Sandbox Feature
│           │   │   ├── domain/
│           │   │   │   ├── model/       # SandboxSnapshot
│           │   │   │   └── usecase/     # CreateSandbox, LoadSnapshot, ApplySimulation
│           │   │   └── presentation/
│           │   │       └── SandboxViewModel.kt
│           │   │
│           │   └── budget/               # Budget Calculation Feature
│           │       ├── domain/
│           │       │   ├── model/       # SpendingPool, BudgetSummary
│           │       │   └── usecase/     # CalculateSpendingPool, ApplyRollover
│           │       └── presentation/
│           │           └── BudgetViewModel.kt
│           │
│           ├── core/
│           │   ├── database/
│           │   │   ├── Database.kt      # SQLDelight Database definition
│           │   │   └── queries/          # Generated query interfaces
│           │   ├── network/             # Future: API client
│           │   │   └── BudgetApiClient.kt
│           │   └── utils/
│           │       ├── DateUtils.kt
│           │       └── CurrencyUtils.kt
│           │
│           └── navigation/               # Navigation setup
│               └── NavigationGraph.kt
│
├── androidMain/
│   └── kotlin/
│       └── com/budgetcalendar/
│           ├── MainActivity.kt
│           ├── di/
│           │   └── AndroidModule.kt     # Android-specific DI
│           └── ui/
│               ├── theme/
│               │   └── Theme.kt
│               └── screens/
│                   └── (Android screens)
│
└── iosMain/
    └── kotlin/
        └── com/budgetcalendar/
            ├── MainViewController.kt
            ├── di/
            │   └── IosModule.kt          # iOS-specific DI
            └── ui/
                └── (SwiftUI Views rendered from Kotlin)
```

## API Endpoints (Future Backend)

The following endpoints are planned for future cloud sync functionality. The app will work fully offline using local SQLite, with optional sync to these endpoints.

### Accounts API

| Method | Endpoint | Description | Auth Level |
|--------|----------|-------------|------------|
| GET | /api/v1/accounts | List all accounts | User |
| POST | /api/v1/accounts | Create new account | User |
| GET | /api/v1/accounts/:id | Get account by ID | User |
| PUT | /api/v1/accounts/:id | Update account | User |
| DELETE | /api/v1/accounts/:id | Delete account | User |

### Transactions API

| Method | Endpoint | Description | Auth Level |
|--------|----------|-------------|------------|
| GET | /api/v1/transactions | List transactions (with date range) | User |
| POST | /api/v1/transactions | Create transaction | User |
| GET | /api/v1/transactions/:id | Get transaction by ID | User |
| PUT | /api/v1/transactions/:id | Update transaction | User |
| PATCH | /api/v1/transactions/:id/state | Update transaction state (confirm/cancel) | User |
| DELETE | /api/v1/transactions/:id | Delete transaction | User |

### Sandbox API

| Method | Endpoint | Description | Auth Level |
|--------|----------|-------------|------------|
| POST | /api/v1/sandbox/snapshot | Create sandbox snapshot from current state | User |
| GET | /api/v1/sandbox | Get current sandbox state | User |
| POST | /api/v1/sandbox/transactions | Add simulation transaction to sandbox | User |
| DELETE | /api/v1/sandbox | Clear sandbox and return to live data | User |

### Budget API

| Method | Endpoint | Description | Auth Level |
|--------|----------|-------------|------------|
| GET | /api/v1/budget/summary | Get current budget summary | User |
| GET | /api/v1/budget/pool | Get spending pool calculations | User |

## Request/Response Schemas

> **Note**: The API schemas below are for **future cloud sync** functionality. The internal database (SQLDelight) uses INTEGER auto-increment IDs for simplicity.

### Account
```typescript
interface Account {
  id: string;              // UUID (external API) / INTEGER (internal DB)
  name: string;            // Display name
  type: AccountType;       // CHECKING, SAVINGS, CREDIT_CARD, CASH, INVESTMENT
  balance: number;         // Current balance in cents
  isSpendingPool: boolean; // Contributes to "Safe to Spend"
  createdAt: number;       // Unix timestamp (ms)
  updatedAt: number;       // Unix timestamp (ms)
}

enum AccountType {
  CHECKING = "CHECKING",
  SAVINGS = "SAVINGS",
  CREDIT_CARD = "CREDIT_CARD",
  CASH = "CASH",
  INVESTMENT = "INVESTMENT"
}
```

### Transaction
```typescript
interface Transaction {
  id: string;              // UUID (external API) / INTEGER (internal DB)
  accountId: string;       // FK to Account
  destinationAccountId?: string; // For TRANSFER type
  amount: number;          // Amount in cents (positive = credit, negative = debit)
  description: string;     // User description
  date: number;            // Unix timestamp (ms)
  type: TransactionType;   // INCOME, EXPENSE, TRANSFER
  state: TransactionState; // PENDING, CONFIRMED, OVERDUE, CANCELLED
  category?: string;       // Optional categorization
  linkedTransactionId?: string; // Links paired transfer transactions
  isSandbox: boolean;      // Is sandbox transaction
  createdAt: number;       // Unix timestamp (ms)
  updatedAt: number;       // Unix timestamp (ms)
}

enum TransactionState {
  PENDING = "PENDING",     // Scheduled, reserves funds
  CONFIRMED = "CONFIRMED", // Actually occurred
  OVERDUE = "OVERDUE",     // Passed scheduled date, not confirmed
  CANCELLED = "CANCELLED"  // Cancelled, funds released
}
```

### Sandbox Snapshot
```typescript
interface SandboxSnapshot {
  id: string;
  name: string;
  createdAt: string;
  transactions: Transaction[];  // Simulated transactions
  accounts: Account[];          // Simulated account states
}
```

### Budget Summary
```typescript
interface BudgetSummary {
  totalLiquidAssets: number;    // Sum of all spending pool accounts
  pendingReservations: number;  // Sum of PENDING transactions
  confirmedSpending: number;    // Sum of CONFIRMED this month
  availableToSpend: number;     // Real-time "Safe to Spend"
  rolloverAmount: number;       // Unspent from previous month
}
```

## Dependency Injection (Koin)

```kotlin
// commonMain/kotlin/com/budgetcalendar/di/KoinModules.kt

val databaseModule = module {
    single { Database(get()) }
    single { get<Database>().accountQueries }
    single { get<Database>().transactionQueries }
}

val repositoryModule = module {
    single<IAccountRepository> { AccountRepositoryImpl(get(), get()) }
    single<ITransactionRepository> { TransactionRepositoryImpl(get(), get()) }
    single<ISandboxRepository> { SandboxRepositoryImpl(get(), get()) }
}

val useCaseModule = module {
    factory { GetAccountsUseCase(get()) }
    factory { CreateAccountUseCase(get()) }
    factory { GetTransactionsForMonthUseCase(get()) }
    factory { CreateTransactionUseCase(get(), get()) }
    factory { ConfirmTransactionUseCase(get(), get()) }
    factory { CalculateSpendingPoolUseCase(get(), get()) }
    factory { CreateSandboxSnapshotUseCase(get()) }
}

val viewModelModule = module {
    viewModel { AccountViewModel(get(), get()) }
    viewModel { TransactionViewModel(get(), get(), get()) }
    viewModel { CalendarViewModel(get(), get()) }
    viewModel { BudgetViewModel(get(), get()) }
    viewModel { SandboxViewModel(get(), get()) }
}
```

## Data Layer Details (SQLDelight)

### Database Schema

```sql
-- Accounts table
CREATE TABLE accounts (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    balance INTEGER NOT NULL DEFAULT 0,
    is_spending_pool INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- Transactions table
CREATE TABLE transactions (
    id TEXT PRIMARY KEY,
    account_id TEXT NOT NULL REFERENCES accounts(id),
    amount INTEGER NOT NULL,
    description TEXT NOT NULL,
    date TEXT NOT NULL,
    state TEXT NOT NULL DEFAULT 'PENDING',
    is_recurring INTEGER NOT NULL DEFAULT 0,
    recurring_rule TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- Sandbox snapshots table
CREATE TABLE sandbox_snapshots (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TEXT NOT NULL
);

-- Sandbox transactions (separate from main transactions)
CREATE TABLE sandbox_transactions (
    id TEXT PRIMARY KEY,
    snapshot_id TEXT NOT NULL REFERENCES sandbox_snapshots(id),
    account_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    description TEXT NOT NULL,
    date TEXT NOT NULL,
    state TEXT NOT NULL DEFAULT 'PENDING'
);
```

### Key Queries

```sql
-- Get all accounts
SELECT * FROM accounts;

-- Get spending pool accounts only
SELECT * FROM accounts WHERE is_spending_pool = 1;

-- Get transactions for date range
SELECT * FROM transactions 
WHERE date BETWEEN :startDate AND :endDate 
ORDER BY date ASC;

-- Get pending transactions (reservations)
SELECT * FROM transactions 
WHERE state IN ('PENDING', 'OVERDUE')
ORDER BY date ASC;

-- Get overdue transactions
SELECT * FROM transactions 
WHERE state = 'OVERDUE' 
AND date < :today;
```

## Internal Dependencies

| Layer | Depends On |
|-------|------------|
| Presentation (ViewModels) | Domain (Use Cases), Core (DI) |
| Domain (Use Cases) | Domain (Models, Repository Interfaces) |
| Data (Repository Impl) | Core (Database, Queries), Domain (Models) |
| Core (Database) | SQLDelight Runtime |

## External Dependencies

| Library | Purpose |
|---------|---------|
| SQLDelight | SQLite database with type-safe queries |
| Koin | Dependency injection |
| Kotlin Coroutines | Asynchronous operations |
| Kotlin Flow | Reactive data streams |
| kotlinx-serialization | JSON serialization (for future API) |
| Ktor Client | HTTP client (future backend sync) |

## State Management

The app uses **Unidirectional Data Flow (UDF)** pattern:

1. **UI State**: Immutable data class representing screen state
2. **Intent**: User actions dispatched to ViewModel
3. **Processing**: Use cases execute business logic
4. **Result**: Repository fetches/persists data
5. **Update**: Flow emits new state to UI

```
┌─────────┐    Action    ┌───────────┐   Use Case   ┌────────────┐
│   UI    │ ───────────► │ ViewModel │ ──────────► │ Repository │
└─────────┘              └───────────┘             └────────────┘
     │                         │                           │
     │                         ▼                           │
     │                  ┌───────────┐                      │
     │◄──────────────── │  UI State │ ◄───────────────────┘
     │    Flow         └───────────┘   (State Update)
```
