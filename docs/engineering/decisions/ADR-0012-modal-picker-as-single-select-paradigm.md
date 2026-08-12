---
id: ADR-0012
title: Modal picker as single select paradigm
status: Active
date: 2026-08-12
---

# ADR-0012: Modal picker as single select paradigm

## Decision

- Form fields that select one value from a finite list use a modal picker, not an anchored dropdown.
- Label-list selects use `BcSelectField` to open `BcOptionPickerModal`; account selects use `AccountPickerModal`. Both picker paths use `BcSheetShell`.
- `DropdownMenu` and `ExposedDropdownMenuBox` are not used for form selects. `DropdownMenu` remains valid for transaction action overflow in `TransactionDetailsSheet`; that menu chooses actions, not a field value.
- `AccountPickerModal` keeps its direct dependency on the accounts domain `Account` model so it can show account name, balance, type badge, and selected state.

## Why

- One modal interaction pattern removes competing anchored-dropdown and modal-select behavior across form sheets.
- Modal rows provide stable touch targets and selected semantics while giving account choices room for balance and type context.
- `BcOptionPickerModal` keeps generic label-list selection independent of feature domain models; `AccountPickerModal` preserves useful account-specific context without forcing a generic abstraction.
- Existing nested picker usage in transaction and sandbox sheets demonstrates that modal pickers work from an already-open sheet.

## Affects

Docs:

- `docs/engineering/conventions.md`
- `docs/engineering/traps.md`
- `docs/engineering/decisions/README.md`

Code:

- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcSheetShell.kt:29-116`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcOptionPickerModal.kt:28-103`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/AccountPickerModal.kt:29-124`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/form/BcSelectField.kt:16-46`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountFormSheet.kt:134-153`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/creditcard/presentation/components/CreditCardPaymentSheet.kt:109-145`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/recurring/presentation/components/RecurringFormSheet.kt:130-213`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt:282-336`
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionDetailsSheet.kt:53-83`

## Consequences

- Good: form selects share one discoverable modal interaction and accessibility model.
- Good: generic options expose selected state; account options retain balance and type context.
- Cost: selecting a value opens a second modal sheet from a form sheet.
- Cost: `AccountPickerModal` remains coupled to `Account` and accounts-domain presentation details.
- Constraint: action overflow menus are not field selects and may continue using `DropdownMenu`.

## Read when

- adding or changing a form field that selects one value from a list
- changing `BcSelectField`, `BcOptionPickerModal`, or `AccountPickerModal`
- deciding whether a `DropdownMenu` is appropriate in form UI
- changing account picker row context or selected semantics
- changing nested modal-sheet behavior
