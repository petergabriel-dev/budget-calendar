# Account and Credit Card Sheets

**Status:** Implemented
**Surface:** Accounts overlays  
**Layer:** Feature overlays

## Account Form

### Add

- Title: `Add Account`.
- Fields: Name, Type, Initial Balance, Description, Safe to Spend inclusion.
- Credit card conditionally adds Credit Limit, Closing Day, Due Day.
- Credit cards are excluded from Safe to Spend with clear explanation.
- Primary CTA: `Add Account`.

### Edit

- Title: `Edit Account`.
- Preserve values and type rules.
- Primary CTA: `Save`.
- Separated Delete with confirmation.

Validation remains adjacent: name required, amount valid, credit limit > 0, closing/due day 1–31, description soft cap.

## Credit Card Details

- Header identifies card.
- Summary: current balance, statement balance, limit, available credit, utilization, next due date.
- Actions: `Make Payment`, `Edit Settings`.
- Pending/overdue list reuses transaction rows.
- Empty list: `No pending or overdue expenses for this card.`

## Credit Card Payment

- Title: `Pay {card name}`.
- Suggested payment context.
- Payment Amount + Source Account picker.
- Empty source state explains: add spending-pool account in Accounts.
- Warn when amount exceeds reserved amount.
- Actions: Cancel + Confirm.
- Validate amount > 0 and source account selected.

## Credit Card Settings

- Title: `Credit Card Settings`.
- Fields: Credit Limit, Closing day (1–31), Due day (1–31).
- Actions: Cancel + Save.
- Delete Card is separated/destructive.
- Preserve values on validation error.

## Accessibility, responsive, theme

- Each sheet has named heading, explicit close, labeled fields, inline errors, and focus restoration.
- Payment/settings fields meet `spacing12`; two-field day row stacks when needed.
- All destructive actions state consequence and require confirmation.
- Use `BcSheetShell`, semantic tokens, shared field/action components, and light/dark validation.
