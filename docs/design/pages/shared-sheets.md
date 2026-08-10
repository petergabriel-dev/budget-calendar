# Shared Sheets and Overlays

**Status:** Implemented
**Surface:** Cross-page overlays  
**Layer:** Shared interaction system

## Purpose

Make sheets, pickers, forms, detail views, confirmations, and notification overlays feel like one system. Feature content varies; shell, spacing, dismissal, focus, loading, and action hierarchy stay consistent.

## Common sheet shell

```text
Bottom sheet
    Header: title + explicit close
    Scrollable body
    Sticky action footer when actions exist
```

- Full available width on mobile; bounded width on wider layouts.
- `bgPrimary` sheet surface with existing radius and elevation tokens.
- Visible drag handle is optional; never replace explicit close/action controls.
- Header title identifies task or object: `New Transaction`, `Transaction Details`, `Add Account`, etc.
- Close action meets `spacing12` and has a specific content description.
- Long content scrolls independently; footer actions remain reachable.
- Use one primary action. Secondary action is Ghost/Outline. Destructive action is separated and explicit.
- Sheet body uses consistent vertical token spacing. No local raw dp/px values.

## Overlay state rules

| State | Treatment |
|---|---|
| Loading | Preserve title and structure; token skeletons; expose progress semantics |
| Field validation | Inline message adjacent to invalid field; preserve entered values; focus first invalid field when possible |
| Submit busy | Disable form actions; label becomes `Saving...`, `Adding...`, `Confirming...`, or operation-specific equivalent |
| Submit error | Inline error in sheet; re-enable actions; preserve values |
| Success | Close only after persistence succeeds; parent announces updated content |
| Dirty dismissal | Confirm discard before closing; `Keep editing` remains safe action |
| Destructive action | Confirm consequence in dialog; explicit Cancel and destructive confirm |

Errors from one sheet must not blank underlying page content. Sheet dismissal restores focus to invoking control.

## Sheet inventory

| Sheet/overlay | Invoked from | Primary purpose |
|---|---|---|
| Transaction Form | Home Add, Calendar details Edit | Add/edit live transaction |
| Transaction Details | Home/Calendar transaction row | Inspect, edit, delete transaction |
| Account Form | Accounts Add/card details | Add/edit/delete account |
| Account Picker | Transaction/Account/Sandbox forms | Select account with type/balance context |
| Credit Card Details | Accounts credit tile | Inspect card, pending expenses, payment/settings actions |
| Credit Card Payment | Card details | Pay card from spending-pool account |
| Credit Card Settings | Card details | Edit limit and statement dates |
| Notification Center | Any header bell | Review/dismiss notifications |
| Snapshot Selector | Home Sandbox mode | Select/create/delete sandbox snapshot |
| Add Sandbox Transaction | Sandbox Add | Add planned expense/income/transfer |
| Date Picker / confirmations | Forms and destructive actions | Select date or confirm consequence |

## Transaction Form Sheet

### Add mode

```text
New Transaction
Expense / Income / Transfer
Amount
Name
Date + Account
optional Destination Account
Status
contextual warnings
Add Expense / Add Income / Add Transfer
```

- Account selection uses AccountPicker.
- Credit-card account forces Expense and uses charge-specific context.
- Transfer requires a distinct destination account.
- Status choices expose meaning: Scheduled (Future), Paid/Cleared, or Posted for card charge.
- Primary CTA label reflects operation/type.
- Validation: amount > 0, account required, category/name required for non-transfer, destination required/distinct, credit limit respected.

### Edit mode

- Title: `Edit Transaction`.
- CTA: `Save`.
- Preserve existing values/status/date.
- Show Delete as a separated destructive action after primary Save.
- Dirty close uses discard confirmation.

## Transaction Details Sheet

- Header: `Transaction Details`, close, overflow actions.
- Amount is first detail and includes signed/type meaning.
- Detail rows: Type, Name, Date, Status, Source account, optional Destination account, optional Description.
- Edit and Delete live in overflow menu; Delete confirms `This action cannot be undone.`
- Row/status meaning is text plus semantic treatment, never color alone.

## Account Form Sheet

### Add mode

- Title: `Add Account`.
- Fields: Name, Type, Initial Balance, optional Credit Limit/Closing Day/Due Day for new credit card, Description, Include in Safe to Spend.
- Credit cards are always excluded from Safe to Spend; explain why near control or omit disabled switch with accessible reason.
- Description shows character count and soft cap.
- CTA: `Add Account`.

### Edit mode

- Title: `Edit Account`.
- Preserve account type/balance rules from domain contract.
- CTA: `Save`.
- Delete is separated and confirmed.
- Validation remains adjacent to fields.

## Credit card sheet stack

### Credit Card Details

- Header identifies card account.
- Summary rows: Current Balance, Statement Balance, Credit Limit, Available Credit, Utilization, Next Due Date.
- `Make Payment` and `Edit Settings` are primary/secondary actions.
- Pending & Overdue Expenses list uses shared transaction rows.
- Empty state: `No pending or overdue expenses for this card.`

### Credit Card Payment

- Title: `Pay {card name}`.
- Show suggested payment context.
- Fields: Payment Amount, Source Account picker.
- Warn when payment exceeds reserved amount; never silently block without explanation.
- Empty source accounts state links back to Accounts guidance.
- Actions: Cancel + Confirm. Confirm disabled only when no source account; validation handles invalid amount.

### Credit Card Settings

- Title: `Credit Card Settings`.
- Fields: Credit Limit, Closing day (1-31), Due day (1-31).
- Actions: Cancel + Save.
- Delete Card is separated and destructive.
- Invalid day/amount errors stay adjacent to fields.

## Notification Center Sheet

- Header: `Notifications` plus `Clear all` when notifications exist.
- Empty state: `No new notifications`.
- Notification rows expand in place; each row supports dismiss.
- Overdue count uses status badge.
- Expanded content preserves readable hierarchy and action meaning.
- Error remains visible below list; dismiss/clear actions remain reachable.
- Sheet owns notification loading, expansion, and dismissal state.

## Account Picker

- Header title identifies context: `Select Account` or `Select Destination`.
- Rows include account name, balance, account type badge, selected check.
- Minimum row target `spacing12`.
- Selected account is announced; transfer destination excludes selected source account.
- Empty list state explains why selection is unavailable.

## Snapshot Selector Sheet

- Header: `Select Sandbox`.
- Snapshot rows show name, last accessed, active check.
- Empty state: `No snapshots yet.`
- `Create New` opens name dialog with required validation.
- Long press is not the only deletion path; provide accessible overflow/action equivalent in implementation.
- Delete confirmation names snapshot consequence: removes snapshot and sandbox transactions.

## Add Sandbox Transaction Sheet

```text
New Sandbox Transaction
Expense / Income / Transfer
Amount
Name
Date + Account
optional Destination Account
Add Expense / Add Income / Add Transfer
```

- Uses sandbox-specific destination and validation rules.
- Dirty close confirms discard.
- Account/date pickers reuse shared patterns.
- Snapshot is required; missing snapshot error identifies parent action.

## Accessibility contract

- Every sheet has a named heading and explicit close action.
- Modal focus is trapped while open and returned to invoker on dismissal.
- Scrollable content exposes scroll semantics; footer remains reachable.
- Every form field has label, value, unit/format hint, error, and required state where applicable.
- Every destructive action states consequence before confirmation.
- Confirmation dialogs have safe default focus on Cancel/Keep editing.
- All buttons, icon buttons, picker rows, and toggles meet `spacing12` (48dp).
- Error/success/loading announcements use semantics/live regions and not color alone.

## Responsive

- Mobile-first, full available width.
- Sheets can fill available height for long forms.
- Body scrolls; footer actions remain visible.
- Two-field rows may stack when width is tight.
- No fixed sheet, input, or row widths.
- Wider layouts may use bounded sheet width; no multi-column form galleries.

## Theme and token rules

- Sheet surfaces use `bgPrimary`; grouped content uses `bgSurface`.
- Primary action uses `bgDark` + `textInverted`.
- Destructive action uses approved error semantic and explicit text.
- Borders, focus, disabled, warning, and success treatments use semantic tokens.
- Validate all overlays in system light/dark themes. Do not add user theme preference in this pass.

## Shipped implementation handoff

`BcSheetShell`, `BcSheetHeader`, `BcSheetFooter`, `BcConfirmDialog`, `BcMetricHero`, and tokenized field/action treatments are implemented under `core/designsystem/component`. Feature ViewModels retain domain state, validation, mutation lifecycle, and callbacks; shared primitives own layout and interaction conventions.
