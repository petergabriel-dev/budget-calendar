# Phase 1: Core Financial Loop

> Account Management → Transaction Management → Safe to Spend Calculation

**Features:** Account Management, Transaction Management, Budget (Safe to Spend)
**Shadow Specs:** specs/shadow/Account.shadow.md, specs/shadow/Transaction.shadow.md, specs/shadow/Budget.shadow.md

---

## Group A: Core Infrastructure

- [x] Scaffold KMP project structure: `commonMain`, `androidMain`, `iosMain` source sets with Gradle Kotlin DSL (Use Skill: kmp) (Reference: specs/ARCHITECTURE.md)
- [x] Configure SQLDelight plugin and `Database.sq` with version 1 initial schema: `accounts`, `transactions`, `monthly_rollovers`, `credit_card_settings` tables (Use Skill: kmp) (Reference: specs/DB_DESIGN.md)
- [x] Set up Koin modules: `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule` stubs in `di/KoinModules.kt` (Use Skill: kmp) (Reference: specs/ARCHITECTURE.md)
- [x] Create `DateUtils.kt` and `CurrencyUtils.kt` in `core/utils/` (Use Skill: kmp) (Reference: specs/ARCHITECTURE.md)

---

## Group B: Account Management

- [x] Define `Account` domain model and `AccountType` enum in `features/accounts/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Define `CreateAccountRequest` and `UpdateAccountRequest` models (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Write `accounts.sq` SQLDelight queries: `getAllAccounts`, `getAccountById`, `getSpendingPoolAccounts`, `insertAccount`, `updateAccount`, `deleteAccount`, `getTotalSpendingPoolBalance` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Define `IAccountRepository` interface in `features/accounts/domain/repository/` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `AccountMapper` (DB entity → domain model) in `features/accounts/data/mapper/` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `AccountRepositoryImpl` in `features/accounts/data/repository/` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `CreateAccountUseCase` with validation (name 1–50 chars, non-CC balance ≥ 0) (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `GetAccountsUseCase` returning `Flow<List<Account>>` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `GetAccountByIdUseCase` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `UpdateAccountUseCase` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `DeleteAccountUseCase` with guard: reject if transactions exist (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `GetSpendingPoolAccountsUseCase` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `CalculateNetWorthUseCase` (assets − liabilities) (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Define `AccountUiState` data class in `features/accounts/presentation/` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Implement `AccountViewModel` wiring all account use cases (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [x] Register account bindings in Koin `repositoryModule` and `useCaseModule` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

---

## Group C: Transaction Management

- [x] Define `Transaction` domain model, `TransactionType` enum, `TransactionStatus` enum in `features/transactions/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Define `CreateTransactionRequest` and `UpdateTransactionStatusRequest` models (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Write `transactions.sq` SQLDelight queries: `getTransactionsByAccount`, `getTransactionsByDateRange`, `getTransactionsByDate`, `getPendingTransactions`, `getOverdueTransactions`, `getConfirmedTransactions`, `insertTransaction`, `updateTransactionStatus`, `deleteTransaction` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Define `ITransactionRepository` interface in `features/transactions/domain/repository/` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `TransactionMapper` (DB entity → domain model) in `features/transactions/data/mapper/` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `TransactionRepositoryImpl` in `features/transactions/data/repository/` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `CreateTransactionUseCase` with transfer linked-pair logic and validation (amount > 0, no same-account transfer, income date not in past, expense not > 30 days future) (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `GetTransactionsUseCase` with date range and type filter support (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `GetPendingTransactionsUseCase` and `GetOverdueTransactionsUseCase` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `UpdateTransactionStatusUseCase` (confirm, cancel, overdue transitions) (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `DeleteTransactionUseCase` with linked-transaction cascade for TRANSFER type (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `MarkOverdueTransactionsUseCase` (PENDING → OVERDUE when date + 1 day past) — triggered on app foreground (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Define `TransactionUiState` data class in `features/transactions/presentation/` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Implement `TransactionViewModel` wiring all transaction use cases (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [x] Register transaction bindings in Koin `repositoryModule` and `useCaseModule` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

---

## Group D: Safe to Spend / Budget

- [x] Define `SpendingPool`, `BudgetSummary`, `MonthlyRollover`, `CreditCardReservation` domain models in `features/budget/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Write budget SQLDelight queries: `getTotalSpendingPoolBalance`, `getSafeToSpendAmount`, `getCreditCardReservedAmount`, `getPendingTransactionsForSpendingPool`, `getRolloverByMonth`, `getAllRollovers`, `insertRollover`, `getCreditCardReservations` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `CalculateSpendingPoolUseCase`: sum pool balances − pending − overdue reservations (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `GetBudgetSummaryUseCase` combining pool total, pending, overdue, confirmed, rollover (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `GetSafeToSpendUseCase` (real-time observable via Flow) (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `SaveRolloverUseCase` and `GetRolloverHistoryUseCase` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `ApplyRolloverUseCase` — adds previous month's rollover to current Safe to Spend (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Define `BudgetUiState` data class in `features/budget/presentation/` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Implement `BudgetViewModel` wiring all budget use cases, observing account and transaction flows (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
- [x] Register budget bindings in Koin `useCaseModule` and `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
