# 36 — Add Transfer Support to TransactionFormSheet

**Feature:** Transfer Between Accounts (UC-003)
**Shadow Spec:** `specs/shadow/Transaction.shadow.md`
**Shadow Spec to Update:** `specs/shadow/TransactionModal.shadow.md` (currently excludes TRANSFER per constraint TM-001)
**Status:** Planned

---

The backend and ViewModel already fully support TRANSFER transactions — `CreateTransactionUseCase.createTransferPair()` creates two linked transactions, `TransactionFormViewModel.validateRequest()` validates destination account rules, and `CreateTransactionRequest.destinationAccountId` exists. Only the UI in `TransactionFormSheet.kt` is missing Transfer: the `BcSegmentedControl` is hardcoded to `["Expense", "Income"]`, type coercion forces TRANSFER → EXPENSE, and `destinationAccountId` is hardcoded to `null`. This prompt adds Transfer to the transaction form UI and updates the shadow spec.

---

## Section 1 — Non-Functional Requirements

- **Performance:** Transfer creation must feel instant — two DB inserts (linked pair) + 1 patch update should complete in < 100ms. UI must not jank when adding/removing the destination dropdown.
- **Data Integrity:** Two linked transactions must both exist or neither. (Existing `createTransferPair()` handles this — non-atomic inserts are a pre-existing risk, not changed in this PR.)
- **Validation:** All existing validation rules apply — destination required for TRANSFER, source ≠ destination, amount > 0, category optional for TRANSFER.
- **UX Consistency:** Destination account dropdown must use the same `BcInputGroup(readOnly=true) + DropdownMenu` pattern as the source account dropdown already in `TransactionFormSheet.kt` (lines 213–247). Do NOT copy the `AccountDropdown` composable from `RecurringFormSheet.kt` — it uses Material3 `OutlinedTextField` which is inconsistent with the design system.
- **Accessibility:** Destination account field must have adequate touch targets and clear labeling for screen readers.

---

## Section 2 — Success Metrics

- **Functional:** User can create a transfer between two different accounts via the TransactionFormSheet, producing two linked transactions in the database.
- **Functional:** Editing an existing transfer pre-populates source account, destination account, and type.
- **Functional:** Switching type away from Transfer clears the destination field and related validation errors.
- **Functional:** Category field is optional when type is Transfer, required for Income/Expense.
- **Functional:** Same-account transfer is rejected with "Cannot transfer to the same account" error.
- **Quality:** All existing `TransactionUseCasesTest` and `TransactionFormViewModel` tests continue to pass (no regressions).
- **Quality:** New validation edge cases covered: destination required, same-account rejection, category optional for Transfer.

---

## Section 3 — Risks and Assumptions

### Assumptions

- `TransactionFormViewModel.validateRequest()` is fully correct for TRANSFER — no ViewModel changes needed. Lines 191–198 validate destination required and source ≠ destination; line 200 skips category validation for TRANSFER.
- `TransactionFormViewModel.setType()` already clears `destinationAccountId` validation errors on type change (line 37–43).
- `CreateTransactionUseCase.createTransferPair()` is correct — no use case changes needed.
- `Transaction.destinationAccountId: Long?` is populated for existing transfer transactions, allowing edit-mode pre-population from `initialData?.destinationAccountId`.
- `BcSegmentedControl` supports 3 options visually on mobile screens (~120dp per segment at ~375dp width), which is acceptable for "Expense"/"Income"/"Transfer" labels. The component accepts `List<String>` with `Modifier.weight(1f)` per segment — no code change needed.

### Risks

- **Risk: BcSegmentedControl visual fit.** The design system spec (`DESIGN_SYSTEM.md:380–391`) describes SegmentedControl as "switching between two options." Three options at ~120dp each may feel cramped on smaller screens (360dp width). **Mitigation:** Test on a 360dp-width emulator. If cramped, abbreviate labels or fall back to `TabRow` (matching `RecurringFormSheet` pattern).
- **Risk: TransactionModal.shadow.md explicitly excludes Transfer.** Constraint TM-001 says "Type selector shows only INCOME and EXPENSE — TRANSFER is excluded from this modal." **Mitigation:** Task 1 updates the shadow spec to document Transfer inclusion.
- **Risk: `createTransferPair()` non-atomic.** Two sequential inserts + a patch create a partial-failure window where source exists without a linked destination. **Mitigation:** This is a pre-existing issue, not introduced by this PR. Noted for a future follow-up.

---

## Section 4 — Tasks

### Task 1: Update shadow spec to include Transfer in TransactionModal

- [ ] Update `specs/shadow/TransactionModal.shadow.md` to document Transfer inclusion: (1) Replace constraint TM-001 "Type selector shows only INCOME and EXPENSE — TRANSFER is excluded from this modal" with "Type selector shows INCOME, EXPENSE, and TRANSFER — when TRANSFER is selected, a destination account dropdown appears." (2) Update `TransactionModalLocalState` to include `destinationAccountId: Long?` and `destinationMenuExpanded: Boolean`. (3) Update `BcSegmentedControl` from `["Expense", "Income"]` to `["Expense", "Income", "Transfer"]`. (4) Update the save data flow to conditionally pass `destinationAccountId`. (5) Update CTA labels to include "Add Transfer". (6) Add a constraint: "Destination account dropdown filters out the source account. Category is optional when type is TRANSFER." (Use Skill: kmp) (Reference: specs/shadow/TransactionModal.shadow.md)
  - **Given** the shadow spec currently excludes TRANSFER **When** Transfer is added to the modal **Then** the spec accurately documents the 3-option selector, destination dropdown, and conditional validation
  - **NFRs:** See Section 1

### Task 2: Fix type coercion and derivation to preserve TRANSFER

- [ ] In `TransactionFormSheet.kt`, update `initialType` coercion (lines 75–78) to add a `TransactionType.TRANSFER -> TransactionType.TRANSFER` branch instead of letting it fall through to `EXPENSE`. Update `selectedType` derivation (lines 104–108) to use a `when` expression that maps all three types — INCOME stays INCOME, TRANSFER stays TRANSFER, else EXPENSE — instead of the current `if/else` that collapses TRANSFER to EXPENSE. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
  - **Given** existing Transfer transactions in DB **When** user opens one for editing **Then** `initialType` resolves to `TRANSFER` and the segmented control shows the Transfer tab selected
  - **Given** ViewModel state holds `TRANSFER` **When** `selectedType` is derived **Then** it remains `TRANSFER`, not coerced to `EXPENSE`
  - **NFRs:** See Section 1

### Task 3: Add Transfer to BcSegmentedControl and destination account dropdown

- [ ] In `TransactionFormSheet.kt`: (1) Update `BcSegmentedControl` options from `listOf("Expense", "Income")` to `listOf("Expense", "Income", "Transfer")`. Map `selectedIndex` using `when`: EXPENSE=0, INCOME=1, TRANSFER=2. Map `onSelectedIndexChange` using `when`: 0→EXPENSE, 1→INCOME, 2→TRANSFER. When type changes away from TRANSFER, set `destinationAccountId = null`. Always clear `localErrors["destinationAccountId"]` on type change. (2) Add `var destinationAccountId by remember(stateKey) { mutableStateOf(initialData?.destinationAccountId) }` after `selectedAccountId` state. (3) Add `var destinationMenuExpanded by remember(stateKey) { mutableStateOf(false) }` after `accountMenuExpanded` state. (4) Add a conditional destination account dropdown after the source account `Box` block (after line 247, before `if (showDatePicker)`). Use the same `BcInputGroup(readOnly=true) + DropdownMenu` pattern as the source account dropdown (lines 213–247), but with: label "Destination Account", filtered account list excluding source (`uiState.availableAccounts.filter { it.id != selectedAccountId }`), `destinationAccountId` as selected value, `destinationMenuExpanded` for expansion state. Only show when `selectedType == TransactionType.TRANSFER`. (5) In the source account `DropdownMenuItem` onClick, add `if (destinationAccountId == account.id) { destinationAccountId = null }` to clear destination when source changes to match destination. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md, specs/shadow/TransactionModal.shadow.md)
  - **Given** user selects "Transfer" in segmented control **When** destination dropdown appears **Then** it shows all accounts except the currently selected source account
  - **Given** user changes source account to match destination **When** source account dropdown onClick fires **Then** `destinationAccountId` is cleared and the user must re-pick a destination
  - **Given** user switches type from Transfer to Expense **When** segmented control onChange fires **Then** destination dropdown disappears and `destinationAccountId` is cleared
  - **NFRs:** No UI jank when dropdown appears/disappears

### Task 4: Update validation and save logic for Transfer type

- [ ] In `TransactionFormSheet.kt`: (1) Add validation after the `accountId == null` check (around line 329): `if (selectedType == TransactionType.TRANSFER && destinationAccountId == null) { errors["destinationAccountId"] = "Destination account is required" }` and `if (selectedType == TransactionType.TRANSFER && destinationAccountId == selectedAccountId) { errors["destinationAccountId"] = "Cannot transfer to the same account" }`. (2) Make category required check conditional: change `if (categoryInput.isBlank())` to `if (selectedType != TransactionType.TRANSFER && categoryInput.isBlank())` at line 332. (3) Pass `destinationAccountId` conditionally in `onSave`: change `destinationAccountId = null` (line 344) to `destinationAccountId = if (selectedType == TransactionType.TRANSFER) destinationAccountId else null`. (4) Update `showInsufficientFunds` (lines 111–114) to also apply for TRANSFER: change `selectedType == TransactionType.EXPENSE` to `(selectedType == TransactionType.EXPENSE || selectedType == TransactionType.TRANSFER)`. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
  - **Given** user submits Transfer without destination **When** validation runs **Then** error "Destination account is required" is shown
  - **Given** user submits Transfer with same source and destination **When** validation runs **Then** error "Cannot transfer to the same account" is shown
  - **Given** user submits Transfer with empty category **When** validation runs **Then** no category error is shown (category optional for Transfer)
  - **Given** user submits Transfer with amount exceeding source balance **When** form validates **Then** insufficient funds warning is displayed
  - **Given** user submits valid Transfer **When** `CreateTransactionRequest` is built **Then** `destinationAccountId` is set to the selected destination account ID
  - **NFRs:** See Section 1

### Task 5: Add unit tests for Transfer UI validation scenarios

- [ ] Add tests to `TransactionUseCasesTest.kt` covering these scenarios that may not yet be exercised or that validate the full end-to-end Transfer flow: (1) Creating a valid Transfer between two different accounts succeeds and `CreateTransactionUseCase` produces a source transaction with `linkedTransactionId` pointing to the destination transaction. (2) Creating a Transfer where `destinationAccountId` is null fails with "Transfer destination account is required". (3) Creating a Transfer where source and destination are the same account fails with "Cannot transfer to the same account". (4) Creating a Transfer without a category succeeds (category not required for TRANSFER). (5) Creating an EXPENSE without a category fails with the expected validation error (regression test). Use `cocoindex-code` to search `"TransactionUseCasesTest"` to find existing test patterns and repository fakes to reuse. (Use Skill: kmp) (Reference: specs/shadow/Transaction.shadow.md)
  - **Given** all existing tests pass **When** new Transfer tests run **Then** all pass without regressions
  - **NFRs:** Tests use existing fake/mock repository patterns — no new infrastructure needed

---

## Section 5 — Definition of Done

- [ ] All 5 tasks above are implemented and committed
- [ ] Transfer type appears in segmented control and all 3 options work (Expense, Income, Transfer)
- [ ] Destination account dropdown appears conditionally when Transfer is selected
- [ ] Creating a transfer creates two linked transactions in the database
- [ ] Editing an existing transfer pre-populates both source and destination
- [ ] Switching type away from Transfer clears destination and validation errors
- [ ] Category is optional for Transfer, required for Income/Expense
- [ ] Same-account transfer validation works (error: "Cannot transfer to the same account")
- [ ] Insufficient funds warning shows for Transfer and Expense
- [ ] All existing tests pass without regressions
- [ ] `TransactionModal.shadow.md` updated to document Transfer inclusion
- [ ] `./gradlew :composeApp:testDebugUnitTest` passes with zero failures
- [ ] No critical or high-severity bugs open