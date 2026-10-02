---
id: ADR-0013
title: Derive spending-pool membership from account type
status: Active
date: 2026-08-13
---

# ADR-0013: Derive spending-pool membership from account type

## Decision

- Spending-pool membership derives from `AccountType`, not a persisted per-account override.
- `CHECKING` and `CASH` are spending-pool accounts. `SAVINGS`, `CREDIT_CARD`, and `INVESTMENT` are not.
- `Account.isInSpendingPool` is a computed property backed by `AccountType.isInSpendingPool`.
- Safe-to-Spend pool totals and pending/overdue reservations use SQL predicates `a.type IN ('checking', 'cash')`.
- Credit-card payment-source eligibility is separate from spending-pool membership. Payment sources include every account type except `CREDIT_CARD`, so savings accounts can pay cards.
- The old `is_in_spending_pool` column and index are removed in schema migration 13. Account rows and inbound transaction foreign keys are preserved during the table rebuild.

## Why

- Account type already expresses the intended role boundary: checking and cash fund Safe to Spend; savings and investments are assets outside that pool; credit cards are liabilities.
- Removing the mutable override prevents account type and Safe-to-Spend behavior from drifting apart and removes an account-form control with no valid independent business meaning.
- Payment-source eligibility answers a different question from Safe-to-Spend membership. Savings can fund a card payment without contributing its balance to Safe to Spend.
- SQL predicates remain explicit and reactive at the budget/query layer, while domain/UI account lists can reuse the type-derived property.

## Affects

Docs:

- `docs/engineering/architecture.md`
- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/Account.kt:3-30`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:51-62`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:1-14`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/accounts.sq:1-47`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:1-22`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq:88-109`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/13.sqm:1-20`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/CreditCardViewModel.kt:265-279`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/CreditCardUiState.kt:12-22`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/budget/presentation/BudgetViewModel.kt:24-31` (retired by ADR-0014)
- `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:460-570`

Related ADRs:

- [ADR-0001](ADR-0001-computed-account-balance-from-ledger.md)
- [ADR-0006](ADR-0006-credit-card-liability-balance-reservation.md)

## Consequences

- Good: Safe-to-Spend behavior follows account type consistently across domain, budget SQL, and account lists.
- Good: savings remains available as a credit-card payment source without inflating Safe to Spend.
- Good: account creation/editing has no manual Safe-to-Spend membership control to drift from type.
- Good: schema no longer stores a redundant membership flag or index; migration 13 preserves account data and inbound references.
- Cost: changing which account types fund Safe to Spend now requires a domain, SQL, and documentation change rather than a per-account toggle.
- Risk: existing savings accounts previously marked in the old pool flag stop contributing to Safe to Spend after migration; this is intentional correction, with no backfill because the flag is no longer read.
- Risk: future code can conflate pool accounts with card payment sources; keep separate use cases and state names.

## Read when

- changing `AccountType` or Safe-to-Spend membership
- changing budget pool totals or pending/overdue reservation queries
- changing account creation/editing controls related to Safe to Spend
- changing credit-card payment-source selection
- changing the accounts schema or migrations involving account references
- deciding whether savings, investments, or other account types affect Safe to Spend
