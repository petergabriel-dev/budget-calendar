# Bugfix: Cannot Resolve Overdue Transactions from Home Screen

**Bug Report:** `bug-reports/safe-to-spend/2026-04-01_overdue-transaction-cannot-be-resolved.md`

**Affected Features:** Home Screen, Transaction Management

**Shadow Specs:**
- `specs/shadow/Transaction.shadow.md`
- `specs/shadow/Calendar.shadow.md`

---

## Tasks

- [x] **Task 1: Add state variable to track editing transaction** — In `HomeScreen.kt`, add `var editingTransaction by remember { mutableStateOf<Transaction?>(null) }` near line 103 where other state variables are defined. This will track which transaction the user is attempting to resolve when tapping on an overdue transaction. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [x] **Task 2: Update ScheduleTransactionRow onClick handler** — Modify the `onClick` lambda in the `overdueTransactions` items block (around line 221) to set `editingTransaction = transaction` before showing the form. Change from `onClick = { transactionFormViewModel.setType(transaction.type); showAddForm = true }` to `onClick = { editingTransaction = transaction; transactionFormViewModel.setType(transaction.type); showAddForm = true }`. This ensures the tapped transaction is passed to the form. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

- [x] **Task 3: Wire TransactionFormSheet with initialData and proper callbacks** — Update the `TransactionFormSheet` call (lines 286-301) to: (a) pass `initialData = editingTransaction` instead of `null`, (b) update `onSave` to handle both create and replace: `val editing = editingTransaction; if (editing == null) { transactionFormViewModel.submit(request) } else { transactionFormViewModel.replace(existingTransactionId = editing.id, request = request) }`, (c) set `editingTransaction = null` after save completes. Also wire `onCancel` and `onDelete` to properly reset `editingTransaction = null`. Reference the pattern used in `CalendarScreen.kt` lines 175-201 for correct implementation. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md, specs/shadow/Calendar.shadow.md)

- [ ] **Task 4: Verify form opens with correct transaction data** — Run the app and navigate to Home Screen. Create or locate an overdue transaction. Tap on it and verify: (a) the Transaction Form opens with "Edit Transaction" title, (b) amount, category, date, and account are pre-populated with the existing transaction's data, (c) the status section allows changing from "Scheduled" to "Paid / Cleared". (Use Skill: kmp)

- [ ] **Task 5: Verify confirm flow works end-to-end** — After Task 4, change the status to "Paid / Cleared" and submit. Verify: (a) the overdue transaction is now confirmed, (b) the account balance is correctly updated (EXPENSE deducts from account), (c) Safe to Spend correctly reflects the change (reservations decreased). (Use Skill: kmp)

- [ ] **Task 6: Verify cancel flow releases funds** — Create a new overdue transaction and this time tap Cancel/Skip. Verify: (a) the transaction status changes to CANCELLED, (b) Safe to Spend increases by the cancelled amount (funds released back to pool), (c) account balance is unchanged (pending amounts were never applied). (Use Skill: kmp)

- [x] **Task 7: Run tests** — Run `./gradlew :composeApp:allTests` to ensure no regressions. (Use Skill: kmp) ✅ BUILD SUCCESSFUL
