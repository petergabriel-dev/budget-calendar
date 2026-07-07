---
id: ADR-0009
title: Card-native charge sheet and statement cycle display
status: Active
date: 2026-07-07
---

# ADR-0009: Card-native charge sheet and statement cycle display

## Decision

- When the selected source account is a credit card, the add/edit transaction sheet becomes a card-charge form: no Expense/Income/Transfer control, effective type is forced to `EXPENSE`, and any transfer destination is cleared.
- New same-day/past card charges default to confirmed and use the visible status label "Posted"; future-dated card charges default to pending/scheduled.
- Card charges can show a display-only line: `Bills to statement closing {date} · due {date}`. The line is computed from the charge date plus the card's `statement_closing_day` and `payment_due_day`.
- Credit-card settings now expose recurring `statement_closing_day` and `payment_due_day` (1-31). These days are additive settings data and are clamped to each month by `DateUtils`.
- Manual `statement_balance` and `due_date` are retired from settings/detail UI but columns remain and are passed through unchanged on save.
- This decision does not change ledger writes, `signed_amount`, card balance, STS reserve, or net-worth math. Full statement auto-cut/minimum-payment automation remains deferred.

## Why

- Card users are recording charges, not choosing income/transfer semantics from a credit-card source account.
- A posted card charge is the normal current-day/past-day status, while a future card charge cannot honestly be posted yet.
- Statement closing/due days are stable recurring settings; manual one-off statement balance/due date fields drifted and did not affect STS.
- Keeping statement context display-only preserves ADR-0006 and ADR-0008: STS/card balance/net worth remain ledger-derived and transaction-reactive.
- Additive schema keeps migration safe on minSdk 24 and preserves vestigial columns for old installs.

## Affects

Docs:

- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/Database.sq:46-56`
- `src/mobile/composeApp/src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/12.sqm:1-2`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtils.kt:92-141`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/model/CreditCardSettings.kt:3-13`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/model/CreditCardSummary.kt:3-14`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardSettingsForm.kt:83-163`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardDetailSheet.kt:63-73`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:133-145`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:241-263`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:420-480`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:535-542`
- `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/core/utils/DateUtilsStatementCycleTest.kt:1-106`
- `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheetLogicTest.kt:1-74`
- `src/mobile/composeApp/src/androidUnitTest/kotlin/com/petergabriel/budgetcalendar/features/budget/domain/usecase/BudgetFlowObservationIntegrationTest.kt:580-646`

Related ADRs:

- [ADR-0006](ADR-0006-credit-card-liability-balance-reservation.md)
- [ADR-0007](ADR-0007-card-aware-transaction-sheet-over-limit-block.md)
- [ADR-0008](ADR-0008-unified-projected-credit-card-balance.md)

## Consequences

- Good: card charge entry is simpler and avoids invalid income/transfer source semantics.
- Good: users see statement-cycle context without changing ledger/STS behavior.
- Good: short-month day clamping is centralized and tested.
- Good: old statement columns survive migration and pass through unchanged.
- Cost: transaction presentation has card-specific reskin logic.
- Risk: future work could mistake the display-only statement line for accounting logic; keep STS and card balance tied to ADR-0006/ADR-0008.

## Read when

- changing `TransactionFormSheet` behavior for credit-card accounts
- changing credit-card settings UI or `credit_card_settings` schema
- changing statement closing/due date display or month clamping
- reintroducing manual statement balance/due date UI
- adding statement auto-cut, minimum-payment, or payment scheduling logic
- changing STS reserve, card balance, net worth, or `signed_amount` semantics

## Supersedes

- Manual statement balance/due date entry in credit-card settings UI.
- Showing credit-card transaction sheet as a generic Expense/Income/Transfer selector when the source account is a card.
