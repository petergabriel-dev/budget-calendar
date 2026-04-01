# Phase 3: Account & Transaction Management UI

> Compose screens and forms for creating, editing, and deleting accounts and transactions

**Shadow Specs:** specs/shadow/Account.shadow.md, specs/shadow/Transaction.shadow.md

---

## Group A: Account UI State & ViewModel

- [ ] Define `AccountUiState` data class: `accounts: List<Account>`, `balances: Map<Long, Long>`, `netWorth: Long`, `isLoading: Boolean`, `error: String?`, `showForm: Boolean`, `editingAccount: Account?` in `features/accounts/presentation/` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [ ] Implement `AccountViewModel` with `loadAccounts()`, `createAccount(CreateAccountRequest)`, `updateAccount(id, request)`, `deleteAccount(id)` — wires `GetAllAccountsUseCase`, `CreateAccountUseCase`, `UpdateAccountUseCase`, `DeleteAccountUseCase`, `CalculateNetWorthUseCase`; emits `AccountUiState` via `StateFlow` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [ ] Register `AccountViewModel` in Koin `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

---

## Group B: Account UI Components

- [ ] Implement `AccountCard` composable: account name, account type label, formatted balance via `CurrencyUtils`, spending pool indicator chip; amber/red tint for CREDIT_CARD type; calls `onTap` and `onLongPress` (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [ ] Implement `AccountListScreen` composable: scrollable `LazyColumn` of `AccountCard`s, `FloatingActionButton` triggering `onAddAccount`, empty-state onboarding prompt ("Add your first account") when list is empty, net worth summary row at top (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [ ] Implement `AccountFormSheet` composable (modal bottom sheet): name `TextField`, type `DropdownMenu` (CHECKING / SAVINGS / CREDIT_CARD / CASH / INVESTMENT), initial balance `TextField` (numeric), "Include in Safe to Spend" `Switch`; inline validation error text per field; Save / Cancel buttons; Delete button visible only when editing an existing account (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
- [ ] Implement `AccountScreen` composable: hosts `AccountListScreen` + `AccountFormSheet`; collects `AccountUiState` from `AccountViewModel` via `koinViewModel()`; shows `SnackBar` on error (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

---

## Group C: Transaction Form UI State & ViewModel

- [ ] Define `TransactionFormUiState` data class: `availableAccounts: List<Account>`, `selectedType: TransactionType`, `isLoading: Boolean`, `isSubmitting: Boolean`, `error: String?`, `validationErrors: Map<String, String>` in `features/transactions/presentation/` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] Implement `TransactionFormViewModel` with `setType(TransactionType)`, `submit(CreateTransactionRequest)`, `updateStatus(id, status)`, `delete(id)` — wires `CreateTransactionUseCase`, `UpdateTransactionStatusUseCase`, `DeleteTransactionUseCase`, `GetAllAccountsUseCase`; emits `TransactionFormUiState` via `StateFlow` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] Register `TransactionFormViewModel` in Koin `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

---

## Group D: Transaction UI Components

- [ ] Implement `TransactionStatusBadge` composable: pill-shaped badge — amber background + clock icon for PENDING, red background + warning icon for OVERDUE, green background + check icon for CONFIRMED, grey for CANCELLED (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] Implement `TransactionListItem` composable: income amount in green / expense in red (formatted via `CurrencyUtils`), category label, description (truncated), `TransactionStatusBadge`, account name, formatted date; calls `onTap` and `onLongPress` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] Implement `TransactionFormSheet` composable (modal bottom sheet): type selector tab row (INCOME / EXPENSE / TRANSFER), amount `TextField` (numeric), date picker (`DatePickerDialog`), account `DropdownMenu`, destination account `DropdownMenu` visible only for TRANSFER type, category `TextField`, description `TextField` (optional); inline validation error text per field; Save / Cancel buttons; Delete button visible only when editing; insufficient-funds warning banner for EXPENSE (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] Add `FloatingActionButton` to `CalendarScreen` that opens `TransactionFormSheet` pre-set to the currently selected date; wire `TransactionFormViewModel` via `koinViewModel()` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
- [ ] Update `DayTransactionList` to support tap → open `TransactionFormSheet` pre-populated for editing, and long-press → `AlertDialog` confirming deletion; connect to `TransactionFormViewModel` (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)

---

## Group E: Navigation

- [ ] Add bottom `NavigationBar` to `App.kt` with two destinations: **Calendar** (`CalendarScreen`) and **Accounts** (`AccountScreen`); manage selected tab state; remove standalone `CalendarScreen` as root (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
