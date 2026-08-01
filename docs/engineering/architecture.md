# Architecture

Source of truth: current code. Package root: `com.petergabriel.budgetcalendar` (`src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:1`).

## Runtime shape

- Kotlin Multiplatform + Compose shared UI. Android calls `App()` from `MainActivity`: `src/mobile/composeApp/src/androidMain/kotlin/com/petergabriel/budgetcalendar/MainActivity.kt:13-26`.
- iOS embeds the same Kotlin UI through `MainViewController()` -> `App()`: `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`; host wrapper: `src/mobile/iosApp/iosApp/ContentView.swift:5-17`.
- `App()` owns tab shell and routes to Home, Calendar, Accounts, Me screens: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:66-193`.

## Layer flow

```text
Compose UI -> ViewModel -> UseCase -> Repository interface -> RepositoryImpl -> SQLDelight .sq
```

- ViewModels expose immutable `StateFlow` UI state and mutate backing `MutableStateFlow`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountViewModel.kt:21-30`.
- ViewModels collect use-case flows, update state, and surface errors through state: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountViewModel.kt:38-64`.
- Use cases own validation/orchestration and return `Result<T>` for fallible commands: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:13-44`.
- Domain repositories are interfaces in feature domain packages: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/repository/IAccountRepository.kt:8-18`.
- Repository implementations adapt SQLDelight rows to domain models: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:33-63`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:130-140`.
- SQL lives in `.sq` files, not runtime strings; core schema is `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-123`.

## Feature/package layout

Common feature dirs under `features/`: `accounts`, `budget`, `calendar`, `creditcard`, `home`, `notifications`, `recurring`, `sandbox`, `transactions` (verified by directory tree under `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features`).

Typical full features use:

```text
features/<feature>/
  data/          RepositoryImpl, mapper, local entity when needed
  domain/        model, repository interface, usecase
  presentation/  Compose screen/ViewModel/UiState
```

`home` is presentation-only and contains `HomeScreen`, `HomeViewModel`, and `HomeUiState`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation`.

- `HomeViewModel` collects `GetCreditCardSummariesUseCase` and exposes nullable `availableCreditForSpending`; it sums configured-card availability with a per-card zero floor and clears the value when the stream fails: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeViewModel.kt:15-36`.
- Live Home renders `AVAILABLE CREDIT` only when that aggregate is non-null; Sandbox does not render it: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt:79-220`.

## Reactive backbone

- Transaction repository owns `transactionChangedTrigger`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:28-29`; interface exposes it: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/repository/ITransactionRepository.kt:10-12`.
- Transaction mutators emit after create/status/delete/overdue changes: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:127-206`.
- Account and budget repositories subscribe with `onStart { emit(Unit) }.flatMapLatest { ... }` so dependent SQL re-runs after transaction writes: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:24-62`; `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/data/repository/BudgetRepositoryImpl.kt:19-84`.
- Rationale: [ADR-0002](decisions/ADR-0002-reactive-cross-repository-invalidation.md).

## Balance and Safe-to-Spend model

- Account balance is computed from ledger, not stored as mutable total: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:60-75`; base schema has `initial_balance` and transaction `signed_amount`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-28`.
- Spending-pool total uses same formula over confirmed transactions: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:1-13`.
- Sandbox transactions are separate rows in `sandbox_transactions`; main `transactions` has no `is_sandbox` flag: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:71-86`.
- Safe-to-Spend combines pool balance, spending-pool pending/overdue reservations, credit-card projected-unpaid reserve, rollovers, and confirmed transactions. Credit-card reserve is computed in SQL from each card's liability balance (`initial_balance + non-cancelled signed_amount`) with per-card clamp before summing: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:17-64`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-58`.
- Rationale: [ADR-0001](decisions/ADR-0001-computed-account-balance-from-ledger.md).

## Dependency injection

- Koin modules are centralized in `di/KoinModules.kt`.
- Modules: `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:108-211`.
- `initKoin()` installs platform + shared modules: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:213-224`.

## Platform boundary

- Android platform entry initializes Koin with `androidContext` then calls `App()`: `src/mobile/composeApp/src/androidMain/kotlin/com/petergabriel/budgetcalendar/MainActivity.kt:18-26`.
- iOS platform entry initializes Koin if needed then calls `App()` in `ComposeUIViewController`: `src/mobile/composeApp/src/iosMain/kotlin/com/petergabriel/budgetcalendar/MainViewController.kt:7-12`.
- Feature UI belongs in shared Compose unless it is platform host glue. Rationale: [ADR-0003](decisions/ADR-0003-single-compose-ui-android-ios.md).
