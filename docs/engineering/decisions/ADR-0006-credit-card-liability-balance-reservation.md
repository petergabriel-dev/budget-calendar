---
id: ADR-0006
title: Credit-card liability-balance reservation
status: Active
date: 2026-07-06
---

# ADR-0006: Credit-card liability-balance reservation

## Decision

- Credit-card STS reserve is each card's projected unpaid balance: `max(0, -(accounts.initial_balance + SUM(transactions.signed_amount) WHERE status != 'cancelled'))`.
- The STS-total query sums the per-card clamp after `GROUP BY`; it must not clamp only after summing all cards.
- Charges and payments both affect reserve through `signed_amount`. `statement_balance` and `due_date` remain manual payment guidance only.
- Credit-card charges are excluded from overdue marking and overdue notifications; existing overdue card rows migrate to `confirmed`.
- Minimum-payment tracking and statement automation are deferred to a separate feature.

## Why

- Status-based reserve released a card charge when it became `confirmed`, causing STS to over-report while debt still existed.
- ADR-0001 makes card balance ledger-derived; using the same `signed_amount` source for reserve avoids drift and double-count.
- ADR-0002 already supplies transaction-triggered invalidation for transaction-dependent budget queries; no new invalidation path is needed.
- Card payments should free reserve because they reduce the card liability; expense-only reserve SQL ignored payment transfers.
- Card charges are liability events, not overdue spending-pool placeholders.

## Affects

Docs:

- `docs/engineering/architecture.md`
- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`

Code:

- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/budget.sq:24-58`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/credit_card_settings.sq:27-58`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/transactions.sq:25-31`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/data/repository/TransactionRepositoryImpl.kt:182-205`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/11.sqm:1-8`
- `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:263-554`

Related ADRs:

- [ADR-0001](ADR-0001-computed-account-balance-from-ledger.md)
- [ADR-0002](ADR-0002-reactive-cross-repository-invalidation.md)

## Consequences

- Good: confirmed card charges stay reserved until paid.
- Good: card payments reduce reserve without special Kotlin logic.
- Good: overpaid cards clamp to zero and do not offset other cards' debt.
- Good: overdue notifications no longer include card charges.
- Cost: reserve SQL is more subtle; keep per-card `GROUP BY` clamp.
- Risk: any future `signed_amount` sign bug affects card balance and STS reserve.

## Read when

- changing STS, credit-card reserve, card payment, or account balance SQL
- changing transaction status lifecycle or overdue notifications
- adding statement/minimum-payment automation
- editing `signed_amount` semantics or transfer behavior

## Supersedes

- Status-based credit-card reservation that counted only pending/overdue card expenses.
