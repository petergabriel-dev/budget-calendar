# Transaction Form Sheet

**Status:** Implemented
**Surface:** Shared transaction creation/editing  
**Layer:** Feature overlay

## Purpose

Create or edit live transactions with one predictable form, explicit validation, credit-card context, and safe dirty dismissal.

## Modes

- Add: `New Transaction`, type-aware CTA.
- Edit: `Edit Transaction`, `Save`, separated Delete action.
- Credit-card charge: force Expense, show statement/credit impact context.
- Transfer: show distinct Destination Account picker.

## Field order

1. Expense / Income / Transfer segmented control, unless credit-card charge forces Expense.
2. Large Amount input.
3. Name/category input.
4. Date + Account selectors.
5. Destination Account for transfers.
6. Status: Scheduled (Future), Paid/Cleared, or Posted for card charges.
7. Contextual balance/credit warnings.
8. Sticky primary action footer.

## Validation

- Amount must be > 0.
- Account required.
- Name/category required for non-transfer.
- Destination required and distinct for transfer.
- Credit limit exceedance blocks save with explanation.
- Insufficient funds warning remains visible and meaningful.

## Actions

- Close: dirty forms confirm discard.
- Add CTA: `Add Expense`, `Add Income`, or `Add Transfer`.
- Edit CTA: `Save`.
- Delete appears only in edit mode, separated and confirmed.
- Saving disables controls and preserves entered values.

## Accessibility

- Every field has label, format/unit hint, required/error state.
- Segmented control exposes selected transaction type.
- Picker fields announce selected account/date.
- Credit warnings include text meaning, not color alone.
- Footer actions meet `spacing12`.

## Responsive and theme

- Full-width fields; two-field Date/Account row stacks when needed.
- Body scrolls; footer remains reachable.
- Use `BcSheetShell`, `BcSegmentedControl`, `LargeAmountInput`, `BcInputGroup`, `AccountPicker`, and semantic tokens.
- Validate all variants in light/dark themes.
