---
id: ADR-0008
title: Unified projected credit-card balance
status: Active
date: 2026-07-06
---

# ADR-0008: Unified projected credit-card balance

## Decision

- A credit card's displayed balance is its signed projected balance: `accounts.initial_balance + SUM(transactions.signed_amount WHERE status != 'cancelled')`.
- The same projected balance feeds the Accounts credit-card section, per-card row, credit-card detail sheet, and net-worth calculation.
- Credit-card summaries re-query from `transactionChangedTrigger.onStart { emit(Unit) }.flatMapLatest { ... }` per ADR-0002; do not use `combine(trigger)` for transaction-dependent card summaries.
- `reservedAmount` is derived from projected as `max(0, -projected)`; `availableCredit` is derived as `creditLimit - abs(projected)`.
- Net worth sums non-card confirmed balances plus signed projected card balances. Pending card charges lower net worth; pending spending-pool expenses remain excluded until confirmed.
- Safe-to-Spend reserve behavior from ADR-0006 remains unchanged and keeps its separate `budget.sq` query.

## Why

- Settings-only card summaries could stay stale after transaction writes, causing Accounts section totals to disagree with detail-sheet debt.
- Pending card charges are debt when logged, so the card display and net worth should reflect them immediately.
- One signed projected value avoids drift between Accounts, details, available credit, and net worth.
- ADR-0002 already defines the invalidation pattern needed for transaction-dependent query refresh.
- ADR-0006 already defines the STS reserve; this decision unifies display/net-worth semantics without changing STS.

## Affects

Docs:

- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/credit_card_settings.sq:27-46`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/repository/ICreditCardRepository.kt:24-25`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/data/repository/CreditCardRepositoryImpl.kt:76-84`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummariesUseCase.kt:22-49`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/usecase/GetCreditCardSummaryByIdUseCase.kt:26-39`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/data/repository/AccountRepositoryImpl.kt:124-128`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountListScreen.kt:104-122`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardDetailSheet.kt:53-75`
- `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:42-191`

Related ADRs:

- [ADR-0002](ADR-0002-reactive-cross-repository-invalidation.md)
- [ADR-0006](ADR-0006-credit-card-liability-balance-reservation.md)
- [ADR-0007](ADR-0007-card-aware-transaction-sheet-over-limit-block.md)

## Consequences

- Good: Accounts section, card row, detail sheet, and net worth show one consistent card balance.
- Good: pending card charges immediately lower net worth and available credit.
- Good: overpaid cards count as positive credit in net worth instead of being subtracted as liabilities.
- Good: STS remains governed by ADR-0006 and is not coupled to display wording.
- Cost: Account repository depends on credit-card projected balances for net worth.
- Risk: a future regression to settings-only flows or `combine(trigger)` can reintroduce stale card totals.

## Read when

- changing credit-card summary, list, detail-sheet, or available-credit behavior
- changing net-worth calculation
- changing transaction-dependent repository flow operators
- changing credit-card projected SQL or `signed_amount` semantics
- changing Safe-to-Spend reserve and deciding whether display/net-worth semantics should also change

## Supersedes

- Confirmed-only credit-card display balances in Accounts and net worth.
- Settings-only credit-card summary flows that did not refresh on transaction writes.
- Duplicate detail-sheet display of balance plus reserved amount as separate headline rows.
