# Invariants

Must-not-break rules below are verified from current code only.

## Balance is ledger-derived

- Account balance = `initial_balance + SUM(signed_amount)` for confirmed transactions. Account reads use this formula: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:6-21`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:60-75`.
- Spending-pool total uses the same confirmed ledger formula: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:1-13`.
- Do not reintroduce mutable account-balance adjustment. Transaction writes store ledger rows and emit invalidation; they do not mutate an account balance column: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:127-173`.

## Safe-to-Spend formula

- STS = `pool balance - pending reservations - overdue reservations - credit-card reserved`, clamped `>= 0`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:45-50`.
- Confirmed spending is reported, but not subtracted again from STS; confirmed spending is already reflected in computed pool balance: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt:40-63`.
- Pending/overdue reservations include spending-pool expenses only: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:15-22`.
- Credit-card reserved = sum of each card's projected unpaid balance, clamped per card before summing: `max(0, -(initial_balance + SUM(signed_amount) WHERE status != 'cancelled'))`. This includes pending/overdue/confirmed charges and payments; `statement_balance`/`due_date` do not feed STS: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-58`.

## Status lifecycle and overdue marking

- Valid transitions: `PENDING -> OVERDUE | CONFIRMED | CANCELLED`; `OVERDUE -> CONFIRMED | CANCELLED`; `CONFIRMED` and `CANCELLED` are terminal: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt:31-40`.
- Linked transfer rows are status-updated together when `linkedTransactionId` is present: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/UpdateTransactionStatusUseCase.kt:21-28`.
- Pending becomes overdue only when `nowMillis > transaction.date + DateUtils.MILLIS_IN_DAY`, and the overdue-marking query excludes credit-card accounts so card charges stay out of the overdue lifecycle: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq:25-31`, `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:182-205`.
- Overdue marking is invoked through `MarkOverdueTransactionsUseCase`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/MarkOverdueTransactionsUseCase.kt:6-11`.

## App lifecycle recompute sites

- `App()` injects overdue, projection, STS, settings, and notification use cases: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:72-82`.
- Initial launch runs `refreshNotifications(...)` from `LaunchedEffect(Unit)`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:84-95`.
- Later `ON_RESUME` events also run `refreshNotifications(...)` after the first resume is skipped: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:97-120`.
- `refreshNotifications` first marks overdue, then reads overdue transactions, projection, STS, settings, and generates notifications: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/App.kt:196-226`.

## Credit cards never enter the spending pool

- Credit-card account creation forces `isInSpendingPool = false`, even if the request asks for true: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/CreateAccountUseCase.kt:23-31`.
- Updating an account to or as `credit_card` also forces `isInSpendingPool = false`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/usecase/UpdateAccountUseCase.kt:22-32`.

## Transfers are paired rows

- Creating a transfer writes a source row with `signedAmount = -amount`, a destination row with `signedAmount = amount`, then links both IDs: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:59-82`.
- Deleting a transfer deletes the linked row first when found, then deletes the requested row: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/DeleteTransactionUseCase.kt:9-21`.
- Transfer source and destination account must differ: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:17-24`.

## Cancelled and sandbox rows stay out of real calculations

- Main `transactions` has no sandbox flag. Sandbox transactions live only in `sandbox_transactions`, so real account/budget queries need no `is_sandbox` filter: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:14-28`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:71-86`.
- Real account balances include only `status = 'confirmed'`: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:16-20`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:70-74`.
- Real budget totals/reservations include only relevant statuses/types: `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:8-11`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:15-22`, `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-58`.
- Because real calculations filter to `confirmed`, `pending/overdue`, or `status != 'cancelled'`, `cancelled` rows are excluded from balances and reservations by status filters above.

## Input validation at transaction boundary

- Amount must be `> 0`: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:12-15`.
- Transfer requires destination account and source != destination: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:17-24`.
- Income cannot be in the past: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:26-30`.
- Expense cannot be scheduled more than 30 days ahead: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/usecase/CreateTransactionUseCase.kt:32-36`.
