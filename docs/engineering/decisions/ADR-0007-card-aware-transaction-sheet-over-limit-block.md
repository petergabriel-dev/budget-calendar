---
id: ADR-0007
title: Card-aware transaction sheet over-limit block
status: Active
date: 2026-07-06
---

# ADR-0007: Card-aware transaction sheet over-limit block

## Decision

- The add/edit transaction sheet treats credit-card EXPENSE rows as card charges, not cash withdrawals.
- Credit-card source accounts suppress the balance-based insufficient-funds warning used for bank/cash/savings accounts.
- Credit-card EXPENSE rows show card context: available credit when set, balance after charge, and Safe-to-Spend reserve impact.
- When a credit limit exists and the charge exceeds available credit, Save is hard-blocked by both visible error text and disabled Save.
- The escape hatch for a legitimate over-limit charge is to raise the card limit in card settings, then save.

## Why

- A credit card's negative balance is liability, not spendable cash; comparing a charge to that balance produced a false insufficient-funds warning.
- Available credit is already modeled by credit-card summaries and emitted reactively through the existing summaries Flow.
- The over-limit block is deliberate even though most transaction validation is warn-not-block: over-limit card charges conflict with the user's configured credit limit.
- Visible text plus disabled Save avoids color-only/accessibility failure and makes the block reason clear.
- Reusing the existing summaries Flow and pure charge-impact helper avoids new invalidation paths and per-keystroke suspend calls.

## Affects

Docs:

- `docs/engineering/invariants.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/TransactionFormUiState.kt:1-15`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/TransactionFormViewModel.kt:22-39`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/TransactionFormViewModel.kt:180-192`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/model/CreditCardChargeImpact.kt:1-26`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:151-170`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:422-452`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:489-514`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/di/KoinModules.kt:200-204`
- `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/creditcard/domain/model/CreditCardChargeImpactTest.kt:1-42`
- `src/mobile/composeApp/src/commonTest/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/TransactionFormViewModelTest.kt:1-82`

Related ADRs:

- [ADR-0002](ADR-0002-reactive-cross-repository-invalidation.md)
- [ADR-0006](ADR-0006-credit-card-liability-balance-reservation.md)

## Consequences

- Good: card charges no longer show meaningless cash insufficient-funds warnings.
- Good: users see available credit, balance after charge, and STS reserve impact before saving.
- Good: over-limit charge prevention is explicit and accessible.
- Cost: transaction presentation imports credit-card domain summary data.
- Risk: a real over-limit charge cannot be recorded until the user updates the configured limit.

## Read when

- changing add/edit transaction sheet validation or warnings
- changing credit-card summary, available-credit, or reserve-impact behavior
- changing transaction form ViewModel dependencies or state
- revisiting warn-only vs hard-block transaction policy

## Supersedes

- Cash-account insufficient-funds warning applied to credit-card source accounts.
