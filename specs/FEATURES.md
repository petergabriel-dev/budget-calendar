# Budget Calendar - Feature Specifications

## Overview

The Budget Calendar is a personal finance application built with Kotlin Multiplatform that combines calendar-based transaction tracking with account balance management. Users can visualize their financial life on a calendar, track spending against income, and simulate financial scenarios using sandbox mode. The app provides a comprehensive view of personal finances through account management, transaction tracking, and intelligent calculations of available funds.

This specification defines the business logic, validation rules, state management, and edge cases for all features in the Budget Calendar application. Each feature is designed to work independently while maintaining tight integration with related features through well-defined dependencies.

---

## Feature: Account Management

### Description

Account Management provides the foundation for all financial tracking in the Budget Calendar. Users can create multiple accounts representing different financial holdings, categorize them by type, and designate which accounts contribute to the "Safe to Spend" pool. The system calculates net worth by aggregating all account balances and distinguishing between assets and liabilities.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| ACC-001 | Users can create accounts with a name, type, and initial balance | Required |
| ACC-002 | Account types are limited to: Checking, Savings, Credit Card, Cash, Investment | Required |
| ACC-003 | Accounts can be marked as part of the "Safe to Spend" pool via a boolean flag | Required |
| ACC-004 | Account balance is calculated as: initial_balance + sum(credits) - sum(debits) | Required |
| ACC-005 | Net worth is calculated as: sum(all asset accounts) - sum(all liability accounts) | Required |
| ACC-006 | Credit Card accounts are treated as liabilities with negative balances | Required |
| ACC-007 | Account deletion is only allowed if no transactions reference it | Required |
| ACC-008 | Editing an account name does not affect associated transactions | Required |
| ACC-009 | Each account must belong to a single user | Required |
| ACC-010 | Account creation timestamp is recorded automatically | Required |

### Validation

| Field | Type | Rules | Error Message |
|-------|------|-------|---------------|
| name | string | required, max(50), min(1), trim | "Account name is required and must be between 1 and 50 characters" |
| type | enum | required, one of [CHECKING, SAVINGS, CREDIT_CARD, CASH, INVESTMENT] | "Invalid account type. Must be Checking, Savings, Credit Card, Cash, or Investment" |
| initialBalance | decimal | required, range(-999999999.99, 999999999.99), 2 decimal places | "Initial balance must be a valid amount" |
| isInSpendingPool | boolean | required | "Spending pool status is required" |

### Dependencies

| Feature | Relationship | Integration Point |
|---------|--------------|-------------------|
| Transaction Management | depends on | Account ID foreign key in transactions table |
| Safe to Spend Calculation | uses | isInSpendingPool flag for pool calculation |
| Credit Card Handling | uses | Account type CREDIT_CARD for liability treatment |
| Calendar View | uses | Account information for transaction display |

### State Management

- **Persistent State:** SQLite table `accounts` with columns: id (UUID), user_id, name, type, initial_balance, is_in_spending_pool, created_at, updated_at
- **Session State:** Currently selected account for editing, account list sort order
- **UI State:** Loading state during account fetch, form validation state

### Edge Cases

| Case | Condition | Handling |
|------|-----------|----------|
| Delete account with transactions | Transaction count > 0 | Return error: "Cannot delete account with existing transactions. Delete or reassign transactions first" |
| Negative initial balance for non-CC | type != CREDIT_CARD and balance < 0 | Return error: "Asset accounts cannot have negative initial balance" |
| Positive balance on Credit Card | type == CREDIT_CARD and balance > 0 | Allow and interpret as credit balance |
| Duplicate account name | name exists for user | Allow for organizational flexibility |
| Zero accounts | No accounts created | Show onboarding prompt to create first account |
| All accounts removed from pool | isInSpendingPool == false for all | Show $0.00 Safe to Spend with explanation |

---

## Feature: Transaction Management

### Description

Transaction Management is the core operational feature that records all financial activities in the Budget Calendar. Every income, expense, and transfer between accounts is represented as a transaction with specific states that control how they affect account balances. The system automatically manages transaction state transitions and supports both one-time and recurring transactions.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| TXN-001 | Transactions must have: amount, date, account, type, and state | Required |
| TXN-002 | Transaction types are: INCOME, EXPENSE, TRANSFER | Required |
| TXN-003 | Transaction states: PENDING → OVERDUE → CONFIRMED or CANCELLED | Required |
| TXN-004 | Pending transactions automatically transition to OVERDUE when current_date > transaction_date + 1 day | Required |
| TXN-013 | MarkOverdueTransactionsUseCase must be called at app init AND on every foreground resume | Required |
| TXN-005 | Income adds to account balance (positive amount) | Required |
| TXN-006 | Expense subtracts from account balance (negative amount) | Required |
| TXN-007 | Transfer creates two linked transactions: debit from source, credit to destination | Required |
| TXN-008 | Cancelled transactions do not affect balance calculations | Required |
| TXN-009 | Scheduled transactions are created as PENDING and become CONFIRMED on their date | Required |
| TXN-010 | Recurring transactions follow recurrence pattern until cancelled | Required |
| TXN-011 | Linked transactions (transfers) are deleted together | Required |
| TXN-012 | Transaction amount is always positive; type determines direction | Required |

### Validation

| Field | Type | Rules | Error Message |
|-------|------|-------|---------------|
| amount | decimal | required, range(0.01, 999999999.99), 2 decimal places | "Amount must be greater than zero" |
| date | date | required, valid date format | "Transaction date is required" |
| accountId | string | required, exists in accounts table | "Invalid account selected" |
| type | enum | required, one of [INCOME, EXPENSE, TRANSFER] | "Invalid transaction type" |
| category | string | required for INCOME/EXPENSE, max(50) | "Category is required for income and expenses" |
| description | string | optional, max(200) | "Description cannot exceed 200 characters" |
| isRecurring | boolean | optional, default false | "Invalid recurring flag" |
| recurrencePattern | enum | required if isRecurring, one of [DAILY, WEEKLY, BIWEEKLY, MONTHLY] | "Invalid recurrence pattern" |
| destinationAccountId | string | required if type == TRANSFER, exists in accounts | "Destination account is required for transfers" |

### Dependencies

| Feature | Relationship | Integration Point |
|---------|--------------|-------------------|
| Account Management | depends on | Transaction.accountId references Account.id |
| Calendar View | uses | Transaction.date for calendar display |
| Safe to Spend Calculation | uses | PENDING and OVERDUE transactions for deduction |
| Credit Card Handling | uses | EXPENSE on CC account triggers spending pool deduction |
| Sandbox Mode | uses | Transactions copied to sandbox with sandbox_id |

### State Management

- **Persistent State:** SQLite table `transactions` with columns: id, user_id, account_id, destination_account_id, amount, date, type, state, category, description, is_recurring, recurrence_pattern, recurrence_end_date, linked_transaction_id, sandbox_id, created_at, updated_at
- **Session State:** Currently selected date range filter, transaction type filter, category filter
- **UI State:** Selected transaction for editing, list view mode (calendar/list), bulk selection mode

### Edge Cases

| Case | Condition | Handling |
|------|-----------|----------|
| Overdue transition | current_date > transaction_date + 1 and state == PENDING | Auto-update state to OVERDUE via daily job |
| Transfer to same account | source_account == destination_account | Return error: "Cannot transfer to the same account" |
| Insufficient funds | EXPENSE on account with balance < amount | Allow but show warning banner |
| Cancel pending transaction | state == PENDING and user cancels | Update state to CANCELLED, restore deducted amount |
| Confirm overdue transaction | state == OVERDUE and user confirms | Update state to CONFIRMED |
| Delete recurring transaction | isRecurring == true | Prompt: "Delete this occurrence" or "Delete all occurrences" |
| Past date for new income | date < today and type == INCOME | Return error: "Cannot record income in the past" |
| Future date for expense | date > today + 30 days | Return error: "Cannot schedule expenses more than 30 days ahead" |

---

## Feature: Calendar View

### Description

Calendar View provides the primary user interface for visualizing financial activities in the Budget Calendar. The calendar displays a monthly grid where each day shows a summary of financial activity, including total income, total expenses, and visual indicators for transaction states. Users can navigate between months, tap on specific days to see detailed transactions, and quickly identify pending or overdue items.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| CAL-001 | Calendar displays a month grid with 7 columns (Sunday to Saturday) | Required |
| CAL-002 | Each day cell shows: date number, a tinted background indicating net sign for non-today cells (green = positive, red = negative, transparent = zero/empty), and state dots | Required |
| CAL-003 | Visual indicators: amber dot for pending, red dot for overdue; no confirmed indicator — the tinted background communicates confirmed transactions | Required |
| CAL-004 | Tapping a day opens transaction list for that date | Required |
| CAL-005 | Month navigation via left/right arrows or swipe gesture | Required |
| CAL-006 | Current day always has a bgDark (black) border as the exclusive "today" indicator — no other cell gets a colored border; background is colorError @ 20% when netAmount < 0, colorSuccess @ 20% otherwise | Required |
| CAL-007 | Days from adjacent months are shown as grayed out | Required |
| CAL-008 | Empty days (no transactions) show no border and no state indicators | Required |
| CAL-009 | Calendar fetches transactions for visible months plus one month buffer | Required |
| CAL-010 | Daily summary shows separate income and expense totals | Required |
| CAL-011 | Calendar screen does NOT show a page header, search bar, segmented control, or Safe to Spend card | Required |
| CAL-012 | Month title is left-aligned (bold, 32pt); navigation arrows are right-aligned | Required |
| CAL-013 | End of Month Projection pill shown below month header — month-scoped available-to-spend | Required |
| CAL-014 | Transaction section header reads "Today · {MMM d}" for today, "{DayOfWeek} · {MMM d}" otherwise | Required |
| CAL-015 | End of Month Projection = pool balance ± pending/overdue transactions due within current month only | Required |

### Validation

| Field | Type | Rules | Error Message |
|-------|------|-------|---------------|
| selectedDate | date | required, valid date | "Invalid date selection" |
| currentMonth | date | required, first day of month | "Invalid month view" |

### Dependencies

| Feature | Relationship | Integration Point |
|---------|--------------|-------------------|
| Transaction Management | depends on | Query transactions by date range |
| Account Management | uses | Account names and types in transaction details |
| Sandbox Mode | uses | Toggle between real and sandbox transactions |

### State Management

- **Persistent State:** None (derived from transactions query)
- **Session State:** Currently viewed month, selected date
- **UI State:** Animation state for month transitions, expanded day details

### Edge Cases

| Case | Condition | Handling |
|------|-----------|----------|
| Month with no transactions | All days have no transactions | Show empty calendar with $0.00 |
| Very long transaction list | > 20 transactions on single day | Show "View all X transactions" link |
| Timezone differences | User in different timezone | Use device local timezone |
| Leap year February | February 29 on leap years | Display correctly |
| Week starts on Monday option | User preference | Support configurable start day |

---

## Feature: Safe to Spend Calculation

**Shadow Spec:** `specs/shadow/Budget.shadow.md`

### Description

Safe to Spend is the central calculation that shows users how much money is actually available for spending. This feature aggregates all accounts in the spending pool, accounts for pending transactions, and implements monthly rollover logic. The calculation provides a real-time view of available funds, helping users make informed spending decisions.

The **Daily Velocity Display** is part of this feature: the Home screen shows a dynamic `₱X / day · N days left` figure below the STS amount, computed as `availableToSpend / daysRemainingInMonth` (where today counts as a remaining day). On the last day of the month the figure turns red with a `⚠` icon and a tappable tooltip warning. This requires `DateUtils.daysRemainingInMonth()` and updates to `HeroSafeToSpend`.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| SFS-001 | Safe to Spend = sum(balances of accounts where isInSpendingPool == true) | Required |
| SFS-002 | Pending transactions are deducted from Safe to Spend | Required |
| SFS-003 | Cancelled transactions are NOT deducted | Required |
| SFS-004 | Overdue transactions remain deducted until confirmed or cancelled | Required |
| SFS-005 | Monthly rollover: unspent Safe to Spend carries to next month | Required |
| SFS-006 | Rollover amount = Safe to Spend at end of month - confirmed expenses | Required |
| SFS-007 | Sandbox mode Safe to Spend is calculated independently | Required |
| SFS-008 | Credit Card expenses are deducted immediately when incurred | Required |
| SFS-009 | Calculation updates in real-time when transactions change state | Required |
| SFS-010 | Rollover history is preserved for financial reporting | Required |

### Validation

| Field | Type | Rules | Error Message |
|-------|------|-------|---------------|
| calculationDate | date | required | "Calculation date required" |
| includeSandbox | boolean | optional, default false | "Invalid sandbox flag" |
| accountIds | list | optional, filter specific accounts | "Invalid account list" |

### Dependencies

| Feature | Relationship | Integration Point |
|---------|--------------|-------------------|
| Account Management | depends on | Query accounts where isInSpendingPool == true |
| Transaction Management | depends on | Query PENDING, OVERDUE transactions |
| Sandbox Mode | uses | sandbox_id parameter for sandbox calculation |
| Credit Card Handling | uses | Reserved amounts included in deduction |

### State Management

- **Persistent State:** Monthly rollover amounts in `monthly_rollovers` table; Cached Safe to Spend in `safe_to_spend_cache`
- **Session State:** Current Safe to Spend cached in memory with 30-second TTL
- **UI State:** Loading state during calculation, comparison state |

### Edge Cases

| Case | Condition | Handling |
|------|-----------|----------|
| No accounts in pool | isInSpendingPool == false for all | Show $0.00 with explanation |
| All pending cancelled | pending_total == 0 | Full balance available |
| Month boundary | Calculation on last day | Include rollover preview |
| Negative Safe to Spend | Deductions exceed balance | Show $0.00 with warning |
| Sandbox with no transactions | Only copied real data | Show same as real |

---

## Feature: Sandbox Mode (What-If)

**Shadow Spec:** `specs/shadow/Sandbox.shadow.md`
**Status:** In Progress (domain layer complete; UI layer pending)

### Description

Sandbox Mode is a persistent, multi-transaction budget planning scratchpad. Users create a named snapshot of their current Safe to Spend, then stack hypothetical income and expense transactions on top of it. Projected STS updates reactively after each addition. A comparison delta shows how the sandbox STS differs from reality. Plans persist between sessions and individual sandbox transactions can be promoted to real pending transactions.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| SAN-001 | Sandbox creates a point-in-time snapshot storing the real STS value at creation | Required |
| SAN-002 | Sandbox transactions are isolated and never affect real Safe to Spend | Required |
| SAN-003 | Users can add both hypothetical income and expense transactions to a sandbox | Required |
| SAN-004 | Projected STS updates reactively (no manual refresh) after every sandbox transaction add or remove | Required |
| SAN-005 | Comparison delta (sandbox STS vs real STS) is shown inline on the sandbox home view | Required |
| SAN-006 | Multiple sandboxes can exist simultaneously | Required |
| SAN-007 | Sandbox inherits real account list at creation for use in the Add Transaction account selector | Required |
| SAN-008 | Projected STS = initialSafeToSpend + sum(income amounts) - sum(expense amounts) | Required |
| SAN-009 | Sandbox can be deleted without affecting real data; deletion cascades to sandbox_transactions | Required |
| SAN-010 | Individual sandbox transactions can be promoted to real PENDING transactions | Required |
| SAN-011 | Sandbox expires after 30 days of inactivity; expiry runs at app start only | Required |
| SAN-012 | Home screen segmented control labels are "Live Budget" (left) and "Sandbox" (right) | Required |
| SAN-013 | Selecting "Sandbox" replaces home content with: snapshot selector pill, projected STS hero, comparison delta row, sandbox transaction list with Add button | Required |
| SAN-014 | Sandbox data persists in DB across sessions; leaving sandbox mode does not clear sandbox data | Required |
| SAN-015 | Snapshot selector pill opens a bottom sheet of all snapshots ordered by last_accessed_at DESC with a "Create New" option | Required |

### Validation

| Field | Type | Rules | Error Message |
|-------|------|-------|---------------|
| name | string | required, max(50), min(1) | "Sandbox name is required (max 50 characters)" |
| description | string | optional, max(200) | "Description cannot exceed 200 characters" |
| amount | Long (cents) | required, > 0 | "Amount must be greater than zero" |
| category | string | required, max(50) | "Category is required" |
| type | enum | INCOME or EXPENSE only | "Transaction type must be Income or Expense" |

### Dependencies

| Feature | Relationship | Integration Point |
|---------|--------------|-------------------|
| Account Management | uses | Account list for Add Transaction account selector |
| Transaction Management | uses | PromoteTransaction inserts into real transactions table |
| Safe to Spend Calculation | uses | Real STS value needed at snapshot creation and for comparison |

### State Management

- **Persistent State:** `sandbox_snapshots` (id, name, description, initial_safe_to_spend, created_at, last_accessed_at); `sandbox_transactions` (snapshot_id FK, amount, type, status, category, description, account_id, original_transaction_id)
- **Session State:** Currently active snapshot ID; Flow subscriptions for transactions, projected STS, comparison
- **UI State:** isAddTransactionSheetVisible, isSnapshotSheetVisible, isLoading, error

### Edge Cases

| Case | Condition | Handling |
|------|-----------|----------|
| No snapshots exist | First-time sandbox entry | Show empty state with "Create New" CTA |
| projectedSafeToSpend negative | Expenses exceed initialSafeToSpend | Show in colorError with warning icon; do not block further additions |
| Real data changes while sandbox active | New real transactions added/confirmed | comparison.realSafeToSpend updates reactively; delta refreshes automatically |
| Delete sandbox with transactions | sandbox_transactions not empty | Show confirm dialog; cascade handles DB cleanup |
| Promote transaction | sandboxTransactionId valid | Create real PENDING transaction, remove from sandbox, reactive update |
| Sandbox expiration | last_accessed_at older than 30 days at app start | Auto-delete silently; clear selection if it was active |
| Empty sandbox | No transactions added | Show projectedSafeToSpend = initialSafeToSpend with empty state and prominent Add button |

---

## Feature: Credit Card Handling

**Status:** Planned
**Shadow Spec:** `specs/shadow/CreditCard.shadow.md`

### Description

Credit Card Handling manages the unique requirements of credit card accounts within the Budget Calendar. Credit cards are treated as liability accounts, and the system tracks not just the current balance but also the "reserved" amount for upcoming payments. This helps users understand how much of their Safe to Spend is already committed to credit card payments.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| CC-001 | Credit Card accounts are liability accounts with negative balance display | Required |
| CC-002 | When EXPENSE on CC account, deduct from Safe to Spend immediately | Required |
| CC-003 | "Reserved for CC Payment" = sum of all pending/overdue expenses on that CC | Required |
| CC-004 | CC payment is a TRANSFER from Checking to CC account | Required |
| CC-005 | CC payment TRANSFER does NOT double-deduct | Required |
| CC-006 | Multiple CC accounts are tracked separately | Required |
| CC-007 | CC statement balance shown separately from account balance | Required |
| CC-008 | When CC payment confirmed, update reserved amount | Required |

### Validation

| Field | Type | Rules | Error Message |
|-------|------|-------|---------------|
| reservedAmount | decimal | calculated, read-only | N/A |
| statementBalance | decimal | optional, user-entered | "Invalid statement balance amount" |
| dueDate | date | optional, user-entered | "Invalid due date" |
| creditLimit | decimal | optional, user-entered | "Invalid credit limit" |

### Dependencies

| Feature | Relationship | Integration Point |
|---------|--------------|-------------------|
| Account Management | depends on | Account type CREDIT_CARD identification |
| Transaction Management | depends on | Query expenses on CC for reserved calculation |
| Safe to Spend Calculation | modifies | CC expenses deducted from pool |

### State Management

- **Persistent State:** Reserved amounts cached in `cc_reserved` table; `credit_card_settings` for due dates
- **Session State:** Currently selected CC for payment flow
- **UI State:** Reserved amount badge on CC account |

### Edge Cases

| Case | Condition | Handling |
|------|-----------|----------|
| Pay CC more than reserved | payment > reserved | Allow with confirmation |
| CC with no pending expenses | reserved == 0 | Show $0 reserved |
| Multiple CCs | user > 1 CC | Show per-CC amounts |
| Zero balance CC with reserved | balance == 0 but reserved > 0 | Show reserved separately |

---

## Feature Cross-Cutting Rules

### Inter-Feature Dependencies

```
Account Management ─────┬──→ Transaction Management
                        │         │
                        │         ▼
Safe to Spend ◄─────────┴──┬── Calendar View
                          │
                          ▼
Sandbox Mode ◄───────────┴── Credit Card Handling
```

### Shared Business Rules

| Rule | Description | Applies To |
|------|-------------|------------|
| SHR-001 | All monetary amounts use 2 decimal precision | All financial features |
| SHR-002 | Date filtering uses local device timezone | Calendar, Transactions |
| SHR-003 | Currency symbol is user-configurable | All display features |
| SHR-004 | Data export includes all related records | Delete operations |
| SHR-005 | Soft delete for audit trail | All deletions |
| SHR-006 | Timestamps in UTC, displayed in local time | All features |
| SHR-007 | UUID for all entity identifiers | All features |

---

## Error Handling

### Error Types

```kotlin
sealed class BudgetCalendarError {
    data class ValidationError(
        val field: String,
        val message: String,
        val code: String = "VALIDATION_ERROR"
    ) : BudgetCalendarError()
    
    data class NotFound(
        val resource: String,
        val id: String,
        val code: String = "NOT_FOUND"
    ) : BudgetCalendarError()
    
    data class InvalidOperation(
        val reason: String,
        val code: String = "INVALID_OPERATION"
    ) : BudgetCalendarError()
    
    data class CalculationError(
        val message: String,
        val code: String = "CALCULATION_ERROR"
    ) : BudgetCalendarError()
}
```

### Common Error Codes

| Code | Description | HTTP Status | Example |
|------|-------------|--------------|---------|
| VALIDATION_ERROR | Invalid input data | 400 | Missing required field |
| ACCOUNT_NOT_FOUND | Account ID doesn't exist | 404 | Referenced account deleted |
| TRANSACTION_NOT_FOUND | Transaction ID doesn't exist | 404 | Transaction already deleted |
| INSUFFICIENT_BALANCE | Balance too low | 400 | Expense exceeds balance |
| DELETE_WITH_REFERENCES | Cannot delete with linked records | 409 | Account has transactions |
| INVALID_STATE_TRANSITION | Cannot change state as requested | 400 | Confirm already confirmed |
| SANDBOX_EXPIRED | Sandbox older than 30 days | 410 | Attempt to use expired sandbox |

---

## Use Cases

### UC-001: Create New Account

1. User taps "Add Account" button
2. Form displays with fields: Name, Type, Initial Balance, Include in Safe to Spend
3. User fills in required fields
4. System validates input
5. System creates account with generated UUID
6. Account appears in account list
7. If included in spending pool, Safe to Spend recalculates

### UC-002: Record Expense

1. User taps "+" button on calendar or account
2. Selects "Expense" type
3. Enters amount, selects category, enters description
4. Selects account or Credit Card
5. Selects date (defaults to today)
6. System validates input
7. System creates transaction with PENDING state
8. If account in spending pool, Safe to Spend decreases
9. If Credit Card, reserved amount increases
10. Calendar updates with new transaction indicator

### UC-003: Transfer Between Accounts

1. User taps "+" button, selects "Transfer"
2. Selects source account (e.g., Checking)
3. Selects destination account (e.g., Credit Card)
4. Enters transfer amount
5. Selects date
6. System creates two linked transactions
7. If source in spending pool, deducts amount
8. Destination CC increases reserved amount

### UC-004: Use Sandbox Mode

1. User taps "Sandbox" in navigation
2. User taps "Create Sandbox"
3. Names sandbox
4. System creates snapshot of current state
5. User adds hypothetical expense
6. Safe to Spend shows sandbox-adjusted amount
7. User compares with real Safe to Spend
8. User deletes sandbox when done

### UC-005: Pay Credit Card

1. User selects Credit Card account
2. Views reserved amount
3. Taps "Make Payment"
4. System suggests payment amount = reserved
5. User confirms or adjusts amount
6. Selects payment account
7. System creates TRANSFER
8. Reserved amount decreases
9. Checking balance decreases without double-deduction

---

## Feature: Design System Implementation

**Status:** Planned
**Shadow Spec:** `specs/shadow/DesignSystem.shadow.md`

### Description

Implements the 62-component design system as Compose Multiplatform code. Creates a shared theme infrastructure (BudgetCalendarTheme) with design tokens for colors, typography (Outfit + Inter fonts), spacing, border radius, and shadows. All reusable UI components are built as composables in `core/designsystem/` and referenced by all feature screens. This is a presentation-layer-only feature with no database changes.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| DS-001 | All UI code must use BcColors tokens, never raw hex values | Required |
| DS-002 | All text must use BcTypography styles with Outfit/Inter fonts | Required |
| DS-003 | All spacing must use BcSpacing tokens, never raw dp | Required |
| DS-004 | All interactive elements must meet 44x44dp minimum touch target | Required |
| DS-005 | Component names use "Bc" prefix to avoid Material3 collisions | Required |
| DS-006 | All components live in commonMain — no platform-specific UI | Required |
| DS-007 | BcTabBarPill renders the add (+) button centered between tabs — order: [tabs[0..n/2-1]] [+] [tabs[n/2..n-1]], producing Home \| Calendar \| + \| Accounts \| Me | Required |

---

## Feature: Transaction Modal Redesign

**Status:** Planned
**Shadow Spec:** `specs/shadow/TransactionModal.shadow.md`

### Description

Redesigns `TransactionFormSheet` to match the design system spec. Replaces the Material3 `TabRow` + `OutlinedTextField` layout with design-token-native components: `LargeAmountInput` for the amount display, `BcInputGroup` for Name/Date/Account fields, `BcCheckboxRow` for status selection, `BcSegmentedControl` for the Expense/Income toggle, and a single full-width `BcButton` CTA. Removes TRANSFER from the modal. No domain or database changes.

### Business Rules

| Rule | Description | Priority |
|------|-------------|----------|
| TM-001 | Type selector shows only INCOME and EXPENSE — TRANSFER is excluded from this modal | Required |
| TM-002 | "Name" field maps to `category` in `CreateTransactionRequest` | Required |
| TM-003 | Status checkboxes are mutually exclusive: Scheduled (Future) = PENDING, Paid / Cleared = CONFIRMED | Required |
| TM-004 | X button in header replaces the Cancel button — calls onCancel | Required |
| TM-005 | CTA label is "Add Expense" or "Add Income" for create mode, "Save" for edit mode | Required |
| TM-006 | Description field is not shown — always passed as null | Required |
| TM-007 | All tokens must come from BudgetCalendarTheme — no raw hex, dp, or sp values | Required |
