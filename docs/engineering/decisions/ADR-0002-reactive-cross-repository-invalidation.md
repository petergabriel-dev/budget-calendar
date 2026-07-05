---
id: ADR-0002
title: Reactive cross-repository invalidation via transaction trigger
status: Active
date: 2026-07-06
---

# ADR-0002: Reactive cross-repository invalidation via transaction trigger

## Decision

- `ITransactionRepository` exposes `transactionChangedTrigger` as cross-repository invalidation signal.
- Transaction writes emit the trigger after create, status update, delete, and overdue marking.
- Repositories whose SQL depends on transactions wrap SQLDelight flows in `transactionChangedTrigger.onStart { emit(Unit) }.flatMapLatest { ... }` to force fresh queries.

## Why

- Prior reactive model re-emitted cached SQLDelight results instead of re-querying dependent tables; prompt 24 records `combine` as stale for this use: `docs/prompts/24_bugfix_confirmed-transaction-not-updating-balance-and-sts.md:9-21`.
- Flow-observation bug reports identify SQLDelight dependent-table writes not reliably refreshing budget/account consumers: `bug-reports/account-management/2026-04-01_1615_flow-observation-not-triggering.md:82-98`.
- Current code emits explicit write signals: `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:28-29`, `:146`, `:158`, `:173`, `:201-203`.

## Affects

Docs:

- `docs/engineering/architecture.md`
- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/repository/ITransactionRepository.kt:10-12`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:127-206`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:24-62`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/data/repository/BudgetRepositoryImpl.kt:19-84`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:120-124`

## Consequences

- Good: account and budget flows re-query after transaction writes.
- Good: dependency is explicit in DI; consumers do not rely on hidden SQLDelight table observation.
- Cost: account/budget repositories depend on transaction repository.
- Risk: new transaction mutators must emit trigger or dependent flows stale.

## Read when

- adding transaction write paths
- changing repository flow operators
- adding queries dependent on `transactions`
- debugging stale STS, account balance, or budget UI

## Supersedes

- `combine(trigger)` patterns that replay old query values without re-executing SQL.
