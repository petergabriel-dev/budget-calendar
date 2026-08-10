# Transaction Details Sheet

**Status:** Implemented
**Surface:** Shared transaction inspection  
**Layer:** Feature overlay

## Purpose

Show complete transaction context while keeping edit/delete actions explicit and sheet-owned.

## Anatomy

```text
Transaction Details                         close · actions
AMOUNT
signed amount
Type
Name
Date
Status
Source account
optional Destination account
optional Description
```

- Amount is first content block and includes signed/type meaning.
- Detail rows use label-above-value hierarchy.
- Status uses badge/text treatment.
- Overflow actions expose Edit and Delete.

## Behavior

- Close returns to invoking page.
- Edit closes details and opens Transaction Form in edit mode.
- Delete opens confirmation: `This action cannot be undone.`
- Delete success closes sheet and updates parent list.

## Accessibility

- Sheet heading: `Transaction Details`.
- Amount, type, status, account, date, and description form a coherent description.
- Actions have explicit labels; Delete is destructive and confirmed.
- Focus returns to source row after dismissal.
- All controls meet `spacing12`.

## Responsive and theme

- Full-width scrollable body; no fixed detail row width.
- Long values wrap naturally.
- Use semantic text/amount/status/focus tokens and `BcSheetShell`.
