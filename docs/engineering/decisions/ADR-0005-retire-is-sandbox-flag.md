---
id: ADR-0005
title: Retire is_sandbox flag
status: Active
date: 2026-07-06
---

# ADR-0005: Retire is_sandbox flag

## Decision

- Sandbox transactions live solely in `sandbox_snapshots` and `sandbox_transactions`.
- Main-table `transactions.is_sandbox`, `idx_transactions_sandbox`, and all real-query `is_sandbox = 0` filters are retired.
- Main `transactions` rows are real transactions only; future sandbox-in-main-table behavior needs a new explicit column and ADR/update.

## Why

- The flag was vestigial: the only known writer was the deleted orphan `SandboxDetailScreen` path.
- Dual sandbox mechanisms were confusing: sandbox data existed both as separate sandbox tables and as flagged main transaction rows.
- Once the orphan writer was removed, `is_sandbox = 0` filters were no-ops and added maintenance risk to every balance, budget, credit-card, and transaction-feed query.

## Affects

Docs:

- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/architecture.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/credit_card_settings.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/10.sqm`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/Transaction.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/model/CreateTransactionRequest.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/local/TransactionEntity.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/mapper/TransactionMapper.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/sandbox/domain/usecase/PromoteTransactionUseCase.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/recurring/domain/usecase/GenerateMonthlyTransactionsUseCase.kt`

## Consequences

- Good: sandbox source of truth is one mechanism: `sandbox_snapshots` + `sandbox_transactions`.
- Good: real balance, STS, budget, credit-card, and transaction-feed SQL no longer carry dead filters.
- Good: main transaction model has fewer fields and fewer mapper/codegen touch points.
- Bad/risk: any future need to mix sandbox rows into main `transactions` requires a new schema field, migration, and explicit decision.

## Read when

- changing sandbox transaction storage
- adding transaction-like simulation rows
- editing balance, budget, credit-card, or transaction-feed SQL
- considering a sandbox flag on main `transactions`

## Supersedes

- Main-table sandbox flag/filter pattern for `transactions.is_sandbox`.
