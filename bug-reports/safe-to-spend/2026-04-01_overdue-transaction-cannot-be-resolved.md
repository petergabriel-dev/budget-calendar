# Bug: Cannot Resolve Overdue Transactions from Home Screen — "Mark as Paid" Feature Non-Functional

**Date:** 2026-04-01
**Feature:** Home Screen + Transaction Management
**Reported symptom:** Overdue McDo Chicken transaction persists in Safe to Spend pool and cannot be marked as paid/confirmed/cancelled from the Home Screen

---

## Description

When a user taps on an overdue transaction in the **Home Screen's "Overdue" section**, the transaction form opens as a **new transaction** instead of allowing the user to resolve the existing overdue transaction. The user is unable to:

- **Confirm** the transaction (it happened as planned)
- **Cancel** the transaction (it didn't happen — release reserved funds)
- **Edit & Confirm** the transaction (bill was different amount)

This leaves the overdue transaction permanently stuck in the Safe to Spend pool, incorrectly reserving funds.

**Steps to reproduce:**
1. Create an expense transaction (e.g., McDo Chicken) that becomes overdue (date passes by more than 1 day without confirmation)
2. Observe the transaction appears in the Home Screen's "Overdue" section with red styling
3. Tap on the overdue transaction
4. Notice the Transaction Form opens as an empty "New Transaction" form (no data pre-populated)
5. The user cannot confirm, cancel, or edit the overdue transaction from this screen
6. The transaction remains in the overdue state, continuing to affect Safe to Spend

---

## Root Cause Analysis

### Bug Location: HomeScreen.kt — Missing transaction context on tap

**File:** `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/home/presentation/HomeScreen.kt`

**Problem 1: onClick handler does not pass transaction data (lines 217-225)**

```kotlin
items(overdueTransactions, key = { it.id }) { transaction ->
    ScheduleTransactionRow(
        transaction = transaction,
        today = today,
        tz = tz,
        onClick = {
            transactionFormViewModel.setType(transaction.type)
            showAddForm = true
        },
    )
    HorizontalDivider(color = colors.borderSubtle)
}
```

The `onClick` handler only:
1. Sets the transaction type via `transactionFormViewModel.setType(transaction.type)`
2. Shows the form via `showAddForm = true`

**It never passes the actual transaction** (`initialData = transaction`) to the form.

**Problem 2: TransactionFormSheet called with initialData = null (lines 286-301)**

```kotlin
TransactionFormSheet(
    isVisible = showAddForm,
    uiState = formState,
    initialDateMillis = remember { DateUtils.nowMillis() },
    initialData = null,  // <-- Always null, no way to edit existing transaction
    onSetType = transactionFormViewModel::setType,
    onSave = { request ->
        transactionFormViewModel.submit(request)
        showAddForm = false
    },
    onCancel = {
        transactionFormViewModel.clearError()
        showAddForm = false
    },
    onDelete = null,  // <-- No delete callback
)
```

The form is always opened in "create new" mode with `initialData = null`, even when the user intends to resolve an existing overdue transaction.

### Correct Implementation (for reference)

The `CalendarScreen.kt` (lines 175-201) correctly handles transaction editing:

```kotlin
TransactionFormSheet(
    isVisible = showTransactionForm,
    uiState = transactionFormUiState,
    initialDateMillis = selectedDateMillis,
    initialData = editingTransaction,  // <-- Correctly passes existing transaction
    onSetType = transactionFormViewModel::setType,
    onSave = { request ->
        val editing = editingTransaction
        if (editing == null) {
            transactionFormViewModel.submit(request)
        } else {
            transactionFormViewModel.replace(existingTransactionId = editing.id, request = request)
        }
        showTransactionForm = false
        editingTransaction = null
    },
    onCancel = {
        transactionFormViewModel.clearError()
        showTransactionForm = false
        editingTransaction = null
    },
    onDelete = { transaction ->
        transactionFormViewModel.delete(transaction.id)
        showTransactionForm = false
        editingTransaction = null
    },
)
```

---

## Affected Files

| File | Lines | Issue |
|------|-------|-------|
| `features/home/presentation/HomeScreen.kt` | 217-225 | `onClick` doesn't track which transaction is being edited |
| `features/home/presentation/HomeScreen.kt` | 286-301 | `TransactionFormSheet` always opens with `initialData = null` |
| `features/home/presentation/HomeScreen.kt` | 103 | Missing state variable to track `editingTransaction` |

---

## Proposed Solution

### Required Changes

**1. Add state variable to track the transaction being edited (HomeScreen.kt)**

Add near line 103:
```kotlin
var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
```

**2. Update ScheduleTransactionRow onClick to set the editing transaction**

Change from:
```kotlin
onClick = {
    transactionFormViewModel.setType(transaction.type)
    showAddForm = true
},
```

To:
```kotlin
onClick = {
    editingTransaction = transaction
    transactionFormViewModel.setType(transaction.type)
    showAddForm = true
},
```

**3. Update TransactionFormSheet call to pass initialData and wire onSave/onDelete properly**

Change from:
```kotlin
TransactionFormSheet(
    isVisible = showAddForm,
    uiState = formState,
    initialDateMillis = remember { DateUtils.nowMillis() },
    initialData = null,
    onSetType = transactionFormViewModel::setType,
    onSave = { request ->
        transactionFormViewModel.submit(request)
        showAddForm = false
    },
    onCancel = {
        transactionFormViewModel.clearError()
        showAddForm = false
    },
    onDelete = null,
)
```

To:
```kotlin
TransactionFormSheet(
    isVisible = showAddForm,
    uiState = formState,
    initialDateMillis = remember { DateUtils.nowMillis() },
    initialData = editingTransaction,
    onSetType = transactionFormViewModel::setType,
    onSave = { request ->
        val editing = editingTransaction
        if (editing == null) {
            transactionFormViewModel.submit(request)
        } else {
            transactionFormViewModel.replace(existingTransactionId = editing.id, request = request)
        }
        showAddForm = false
        editingTransaction = null
    },
    onCancel = {
        transactionFormViewModel.clearError()
        showAddForm = false
        editingTransaction = null
    },
    onDelete = { transaction ->
        transactionFormViewModel.delete(transaction.id)
        showAddForm = false
        editingTransaction = null
    },
)
```

**4. Clear editing transaction when form is dismissed**

Ensure `editingTransaction` is set to `null` in all exit paths (cancel, successful save, successful delete).

---

## Verification

After the fix:
1. Open Home Screen with overdue transactions
2. Tap on an overdue transaction
3. Transaction form should open with the transaction data pre-populated
4. User can change status to "Paid / Cleared" to confirm, or cancel the transaction
5. Confirming updates account balance correctly (EXPENSE deducts from account, releases from STS)
6. Cancelling releases the reserved funds back to Safe to Spend
