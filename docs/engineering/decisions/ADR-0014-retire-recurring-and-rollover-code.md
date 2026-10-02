---
id: ADR-0014
title: Retire recurring and rollover code; keep dormant tables
status: Active
date: 2026-10-02
---

# ADR-0014: Retire recurring and rollover code; keep dormant tables

## Decision

- Retire the Recurring feature, inactive BudgetScreen and components, monthly-rollover subsystem, credit-card reservation list, and `confirmedSpending` reporting.
- Keep the STS formula unchanged: spending-pool balance minus pending and overdue reservations and credit-card reserve, clamped at zero. Confirmed expenses already affect the computed pool balance.
- Retain the `recurring_transactions` and `monthly_rollovers` table definitions without DROP migrations; existing rows remain untouched.
- Reviving retired capabilities starts from git history and requires a new ADR. Retained tables alone do not imply supported behavior.

## Why

- These screens and repository/use-case paths were unreachable or only used by the retired UI; they added maintenance and query work without live behavior.
- Confirmed spending was redundant STS data, and month-end rollover persistence was not part of the live STS formula.
- Keeping dormant tables avoids a destructive migration while preserving existing on-device data and fresh-install schema compatibility.

## Affects

Docs:

- `docs/engineering/architecture.md`
- `docs/engineering/conventions.md`
- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/dev-workflow.md`
- `docs/engineering/decisions/README.md`
- `docs/engineering/decisions/ADR-0005-retire-is-sandbox-flag.md`
- `docs/engineering/decisions/ADR-0006-credit-card-liability-balance-reservation.md`
- `docs/engineering/decisions/ADR-0012-modal-picker-as-single-select-paradigm.md`
- `docs/engineering/decisions/ADR-0013-type-derived-spending-pool-membership.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/data/mapper/BudgetMapper.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/data/repository/BudgetRepositoryImpl.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/model/BudgetSummary.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/repository/IBudgetRepository.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/CalculateSafeToSpendUseCase.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/presentation/BudgetUiState.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/presentation/BudgetViewModel.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/calendar/domain/usecase/CalculateMonthProjectionUseCase.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/domain/repository/ITransactionRepository.kt`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/recurring_transactions.sq`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq`

## Consequences

- Home STS and low-STS notifications retain existing behavior; the STS flow no longer subscribes to rollover or confirmed-transaction data.
- Rollover-reminder notifications remain based on `CalculateMonthProjectionUseCase` and are independent of `monthly_rollovers`.
- Fresh installs still create the empty dormant tables; no schema migration deletes existing rows.

## Read when

- reviving recurring transactions, the Budget screen, rollover persistence, or per-card reservation-list presentation
- changing the STS formula, confirmed-transaction accounting, or rollover-reminder calculation
- considering removal or migration of either dormant table

## Supersedes

- None. Earlier ADRs retain historical references to retired implementation paths.
