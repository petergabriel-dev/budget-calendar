# Sandbox Sheets

**Status:** Proposed  
**Surface:** Home page, Sandbox mode  
**Layer:** Feature overlays

## Snapshot Selector Sheet

- Heading: `Select Sandbox`.
- Snapshot rows show name, last accessed, and active check.
- Empty: `No snapshots yet.`
- `Create New` opens required-name dialog.
- Delete identifies snapshot and consequence: removes snapshot and sandbox transactions.
- Accessible delete path cannot depend on long press alone.

## Add Sandbox Transaction Sheet

```text
New Sandbox Transaction
Expense / Income / Transfer
Amount
Name
Date + Account
optional Destination Account
primary Add CTA
```

- Snapshot id is required but not user-edited in form.
- Expense/income require Name; transfer requires distinct destination.
- Account/date fields open shared pickers.
- Dirty close confirms discard.
- Validation is adjacent and preserves values.
- CTA label follows type: `Add Expense`, `Add Income`, `Add Transfer`.

## Shared rules

- Use `BcSheetShell` with scrollable body and reachable footer.
- Modal heading, explicit close, focus restoration, and 48dp controls required.
- Loading, submit busy, validation, error, and destructive confirmation states remain local to sheet.
- Light/dark surface, field, badge, focus, and error semantics use shared tokens.
