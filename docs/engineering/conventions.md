# Conventions

Source of truth: current code under package `com.petergabriel.budgetcalendar`.

## Money

- Store/pass money as `Long` cents. Examples: `Account.balance`, `Transaction.amount`, `Transaction.signedAmount`, `BudgetSummary.*Amount` all use `Long`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/Account.kt:17-25`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/Transaction.kt:28-43`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/model/BudgetSummary.kt:3-12`.
- Format display with `CurrencyUtils.formatCents(...)`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/CurrencyUtils.kt:5-19`.
- DB monetary columns are `INTEGER`: `initial_balance`, `amount`, `signed_amount`, `rollover_amount`, notification monetary payloads: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-123`.

## Time

- Store timestamps/dates as `Long` Unix epoch millis. Examples: `Transaction.date`, `createdAt`, `updatedAt`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/Transaction.kt:28-43`.
- Use `DateUtils.nowMillis()`, `startOfDayMillis()`, month bounds, and UTC/local day conversion helpers instead of ad-hoc date math: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtils.kt:14-90`.
- Schema date/time fields are `INTEGER`: accounts/transactions/snapshots/notifications/settings use `created_at`/`updated_at` or date millis columns: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-123`.

## Transaction amounts

- `amount` = unsigned magnitude. `signed_amount` = accounting direction. Schema keeps both: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:14-29`.
- Default signs: income positive, expense negative, transfer source negative, transfer destination positive: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/CreateTransactionRequest.kt:3-29`.
- Balance/accounting reads should use `signed_amount`; reservation totals use magnitude `amount` where pending/overdue obligation size matters: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:62-78`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:16-33`.

## SQLDelight and DB IDs

- SQL belongs in `.sq` query files and numbered `.sqm` migrations under `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/`.
- Current tables use integer IDs; most domain IDs are `Long`. Core tables use `INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT` except singleton `user_settings.id INTEGER NOT NULL PRIMARY KEY DEFAULT 1`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-123`.
- Query methods insert/update via generated SQLDelight APIs, e.g. `insertTransaction`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq:42-58`.

## Koin DI

- DI is centralized in `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt`.
- Installed modules are `platformModule`, `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:106-224`.
- Repositories are `single`, use cases are `factory`, ViewModels use `viewModel`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:120-211`.

## Design system

- Shared Compose design tokens come from `BudgetCalendarTheme`: colors, typography, spacing, radius, shadows: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/theme/BudgetCalendarTheme.kt:14-37`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/theme/BudgetCalendarTheme.kt:89-114`.
- Reusable components use `Bc*` names, e.g. `BcButton`, `BcBadge`, `BcHeader`, `BcTabBarPill`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcButton.kt:36-70`.

## State and errors

- ViewModels expose read-only `StateFlow` and keep mutable state private: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/AccountViewModel.kt:21-30`.
- Fallible use-case commands return `Result<T>` and validate before repository writes: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:13-44`.
