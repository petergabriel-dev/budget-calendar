# 38 — Account Picker Modal for Transaction Form

**Feature:** AccountPickerModal — full-screen account picker replacing dropdowns in TransactionFormSheet and AddSandboxTransactionSheet
**Shadow Spec:** `specs/shadow/TransactionModal.shadow.md`
**Status:** In Progress

---

## Section 1 — Non-Functional Requirements

- **Performance:** Account picker modal opens within 200ms of tap. List scrolling is smooth (60fps) even with 50+ accounts.
- **Usability:** Minimum touch target 44x44dp for each account row. Selected account clearly indicated with checkmark icon.
- **Accessibility:** Each account row announces: "[Account name], [Balance], [Account type]" for screen readers.
- **Token Compliance:** All colors from BcColors, all spacing from BcSpacing, all typography from BcTypography — no raw hex/dp/sp values.

---

## Section 2 — Success Metrics

- **Functional:** Tapping Account or Destination Account field opens full-screen modal. Tapping an account row applies selection and closes modal. Back arrow dismisses without change.
- **Functional:** Selected account shows checkmark icon in the picker row.
- **Functional:** Each account row displays: account name, formatted balance (CurrencyUtils.formatCents), and account type badge (BcBadge).
- **Functional:** Badge variant mapping correct: CHECKING→Info, SAVINGS→Success, CREDIT_CARD→Warning, CASH→Pending, INVESTMENT→Info.
- **Quality:** Compile passes with zero errors.
- **Quality:** Both TransactionFormSheet and AddSandboxTransactionSheet use the shared AccountPickerModal.

---

## Section 3 — Risks and Assumptions

### Assumptions

- Account list already exists in uiState/availableAccounts — no new data fetching needed.
- CurrencyUtils.formatCents is available and works correctly.
- BcBadge component supports all required BadgeVariant values.
- BcHeader component exists and supports back arrow.

### Risks

- **Risk:** Long account lists may cause performance issues. **Mitigation:** Use LazyColumn for efficient rendering.
- **Risk:** Account types may have changed after initial implementation. **Mitigation:** Badge variant mapping defined as enum extension, easily updated.

---

## Section 3.5 — Context Package

Before starting each task, load:
- `specs/shadow/TransactionModal.shadow.md` — primary source of truth
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcBadge.kt` — existing badge implementation
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcHeader.kt` — existing header implementation
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/utils/CurrencyUtils.kt` — balance formatting
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/transactions/presentation/components/TransactionFormSheet.kt` — current implementation with dropdown
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/sandbox/presentation/components/AddSandboxTransactionSheet.kt` — current implementation with dropdown
- `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/domain/model/Account.kt` — Account and AccountType domain models

---

## Section 4 — Tasks

### Task 1: Create AccountPickerModal composable

- [x] Create `AccountPickerModal.kt` in `core/designsystem/component/` (Use Skill: kmp) (Model: Sonnet) (Reference: specs/shadow/TransactionModal.shadow.md)
  - **Given** AccountPickerModal is created in core/designsystem/component/ **When** modal opens with accounts list **Then** each account displays name, formatted balance, and type badge
  - **NFRs:** All tokens from BudgetCalendarTheme. Use LazyColumn for accounts list.
  - **Test Stub:**
    ```kotlin
    // Stub: AccountPickerModal basic structure
    @Composable
    fun AccountPickerModal(
        isVisible: Boolean,
        accounts: List<Account>,
        selectedAccountId: Long?,
        title: String,
        onAccountSelected: (Long) -> Unit,
        onDismiss: () -> Unit,
    ) {
        // TODO: Implement full-screen modal with LazyColumn
        if (!isVisible) return
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = {},
        ) {
            // TODO: BcHeader, LazyColumn, account rows
        }
    }
    ```
  - **Verification Gate:** Compile check passes.
  - **Checkpoint:** Commit after implementation.

### Task 2: Add accountTypeBadgeVariant extension

- [x] Add `accountTypeBadgeVariant` extension function for AccountType (Use Skill: kmp) (Model: Sonnet) (Reference: specs/shadow/TransactionModal.shadow.md)
  - **Given** AccountType enum **When** mapping function is called **Then** returns correct BadgeVariant
  - **NFRs:** Mapping: CHECKING→Info, SAVINGS→Success, CREDIT_CARD→Warning, CASH→Pending, INVESTMENT→Info.
  - **Test Stub:**
    ```kotlin
    // Stub: Badge variant extension
    fun AccountType.toBadgeVariant(): BadgeVariant = when (this) {
        AccountType.CHECKING -> BadgeVariant.Info
        AccountType.SAVINGS -> BadgeVariant.Success
        AccountType.CREDIT_CARD -> BadgeVariant.Warning
        AccountType.CASH -> BadgeVariant.Pending
        AccountType.INVESTMENT -> BadgeVariant.Info
    }
    ```
  - **Verification Gate:** Compile check passes.
  - **Checkpoint:** Commit after implementation.

### Task 3: Update TransactionFormSheet to use AccountPickerModal

- [x] In `TransactionFormSheet.kt`, replace Account dropdown with AccountPickerModal (Use Skill: kmp) (Model: Sonnet) (Reference: specs/shadow/TransactionModal.shadow.md)
  - **Given** TransactionFormSheet has Account field with DropdownMenu **When** user taps Account field **Then** AccountPickerModal opens in full-screen
  - **NFRs:** Maintain existing validation. The showAccountPicker state replaces accountMenuExpanded.
  - **Test Stub:**
    ```kotlin
    // Stub: Replace DropdownMenu with AccountPickerModal call
    if (showAccountPicker) {
        AccountPickerModal(
            isVisible = showAccountPicker,
            accounts = uiState.availableAccounts,
            selectedAccountId = selectedAccountId,
            title = "Select Account",
            onAccountSelected = { id ->
                selectedAccountId = id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }
    ```
  - **Verification Gate:** Compile check passes. Run app, verify modal opens on tap.
  - **Checkpoint:** Commit after implementation.

### Task 4: Update TransactionFormSheet Destination Account to use AccountPickerModal

- [x] In `TransactionFormSheet.kt`, replace Destination Account dropdown with AccountPickerModal (filtered) (Use Skill: kmp) (Model: Sonnet) (Reference: specs/shadow/TransactionModal.shadow.md)
  - **Given** TransactionFormSheet has Destination Account field with DropdownMenu (visible when TRANSFER) **When** user taps Destination Account field **Then** AccountPickerModal opens with source account filtered out
  - **NFRs:** Filter accounts excluding selectedAccountId before passing to picker.
  - **Test Stub:**
    ```kotlin
    // Stub: Destination picker with filter
    val filteredAccounts = uiState.availableAccounts.filter { it.id != selectedAccountId }
    if (showDestinationPicker) {
        AccountPickerModal(
            isVisible = showDestinationPicker,
            accounts = filteredAccounts,
            selectedAccountId = destinationAccountId,
            title = "Select Destination",
            onAccountSelected = { id ->
                destinationAccountId = id
                showDestinationPicker = false
            },
            onDismiss = { showDestinationPicker = false },
        )
    }
    ```
  - **Verification Gate:** Compile check passes. Verify TRANSFER flow works.
  - **Checkpoint:** Commit after implementation.

### Task 5: Update AddSandboxTransactionSheet to use AccountPickerModal

- [x] In `AddSandboxTransactionSheet.kt`, replace Account dropdown with AccountPickerModal (Use Skill: kmp) (Model: Sonnet) (Reference: specs/shadow/TransactionModal.shadow.md)
  - **Given** AddSandboxTransactionSheet has Account field with ExposedDropdownMenuBox **When** user taps Account field **Then** AccountPickerModal opens
  - **NFRs:** Shared component from core/designsystem/component/.
  - **Test Stub:**
    ```kotlin
    // Stub: Replace ExposedDropdownMenuBox with AccountPickerModal in sandbox sheet
    val accounts: List<Account> = remember { emptyList() } // passed as parameter
    if (showAccountPicker) {
        AccountPickerModal(
            isVisible = showAccountPicker,
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            title = "Select Account",
            onAccountSelected = { id ->
                selectedAccountId = id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }
    ```
  - **Verification Gate:** Compile check passes. Verify sandbox ADD flow works.
  - **Checkpoint:** Commit after implementation.

---

## Section 5 — Definition of Done

- [ ] AccountPickerModal created in core/designsystem/component/
- [ ] accountTypeBadgeVariant extension added for AccountType
- [ ] TransactionFormSheet Account field opens AccountPickerModal
- [ ] TransactionFormSheet Destination Account field opens AccountPickerModal (filtered)
- [ ] AddSandboxTransactionSheet Account field opens AccountPickerModal
- [ ] All account rows show: name, formatted balance, type badge
- [ ] Selected account shows checkmark in picker
- [ ] Back arrow dismisses without changing selection
- [ ] Compile passes with zero errors
- [ ] No raw hex/dp/sp values — all tokens from BudgetCalendarTheme
