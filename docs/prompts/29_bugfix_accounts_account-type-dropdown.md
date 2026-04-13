# Bug Fix Plan: Account Type Dropdown Does Not Open on Tap

**Source:** `bug-reports/accounts/2026-04-05_1129_account-type-dropdown-not-working.md`  
**Date:** 2026-04-05

---

## Section 1 — Bug Summary

**Root Cause:** Two compounding issues in `AccountFormSheet.kt` (lines 104–127):

1. **Click swallowed by `OutlinedTextField`** — A `.clickable { typeMenuExpanded = true }` modifier is chained onto the text field, but `OutlinedTextField` internally consumes pointer events for focus and cursor handling. The lambda never fires, so `typeMenuExpanded` stays `false` and the menu never opens.

2. **`DropdownMenu` anchored to wrong parent** — The `DropdownMenu` is placed as a sibling of the `OutlinedTextField` inside the `Column`, not inside a wrapping composable that contains both. `DropdownMenu` anchors to its immediate parent's layout bounds, so even if the menu opened it would appear at the wrong position (likely at the top of the column or off-screen).

**Affected Layer(s):** Presentation only — `features/accounts/presentation/components/AccountFormSheet.kt`

**Severity:** Medium — users cannot change the account type during account creation or editing. The field always silently defaults to `CHECKING`.

---

## Section 2 — Non-Functional Requirements

- **Correctness:** After the fix, tapping the Type field must expand the dropdown and allow selecting any `AccountType` value defined in the spec (`CHECKING`, `SAVINGS`, `CREDIT_CARD`, `CASH`, `INVESTMENT`). All five must be selectable.
- **Correctness:** Selecting `CREDIT_CARD` must still automatically set `includeInSpendingPool = false` via the existing `LaunchedEffect(selectedType)` — this side effect must not be disrupted.
- **Correctness:** The selected type must be correctly passed to `onSave` and ultimately to `CreateAccountRequest` / `UpdateAccountRequest`.
- **Reliability:** The fix must not break the existing validation logic or the form's submit/cancel behaviour.
- **No new dependencies:** The fix uses only APIs already in the Material3 dependency (`ExposedDropdownMenuBox`, `ExposedDropdownMenu`, `ExposedDropdownMenuDefaults`) — no new library versions or imports are required.
- **Regression:** All existing passing tests must continue to pass after the change.

---

## Section 3 — Risks and Assumptions

**Assumptions:**
- The root cause is exclusively in the composable structure; no ViewModel or use-case logic needs to change.
- `ExposedDropdownMenuBox` is available in the project's current Material3 version (`1.10.0-alpha05`) — this API has been stable since M3 1.1.x.
- The `@OptIn(ExperimentalMaterial3Api::class)` annotation already on the composable covers `ExposedDropdownMenuBox` (it is experimental in some M3 versions).
- No other screen reuses the broken type-selector pattern that would need the same fix.

**Risks:**
- `ExposedDropdownMenuBox` in alpha versions of Material3 occasionally has API surface changes. Confirm the exact import path and method names compile before marking done.
- On iOS (via Compose Multiplatform), `ExposedDropdownMenu` rendering may differ from Android — validate on both platforms if possible.
- The `Modifier.menuAnchor()` overload changed signature between M3 versions (added a `type` parameter). Use the no-arg overload to stay compatible with the current version.
- If the sheet's `ModalBottomSheet` clips its content, the dropdown might still be cut off. Verify the menu fully appears within the sheet's visible bounds after the fix.

---

## Section 4 — Fix Tasks

- [ ] **Write a failing Compose UI test for the type dropdown** (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
  - **Given** `AccountFormSheet` is displayed with `isVisible = true` and `initialData = null` **When** the user taps the "Type" field **Then** a dropdown appears showing all five account type options (`Checking`, `Savings`, `Credit Card`, `Cash`, `Investment`)
  - **NFRs:** Test must fail before the fix is applied and pass after. Place in `androidUnitTest` or a dedicated `composeUiTest` source set.
  - **Note:** Use `createComposeRule()` + `onNodeWithText("Type").performClick()` + `onNodeWithText("Savings").assertIsDisplayed()` pattern.

- [ ] **Replace `OutlinedTextField` + `DropdownMenu` with `ExposedDropdownMenuBox` + `ExposedDropdownMenu`** (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
  - **Given** the type selector block in `AccountFormSheet.kt` lines 104–127 **When** refactored to use `ExposedDropdownMenuBox` **Then** the `expanded` state is driven by the box's `onExpandedChange`, `ExposedDropdownMenuDefaults.TrailingIcon` replaces the manual `Text("v")`, and `ExposedDropdownMenu` replaces `DropdownMenu` so the menu anchors correctly to the text field
  - **NFRs:** The `typeMenuExpanded` state variable may be removed; `expanded` is managed by the box. The `LaunchedEffect(selectedType)` block and all `DropdownMenuItem` entries remain unchanged. No changes outside this composable.

- [ ] **Verify all five `AccountType` entries are selectable end-to-end** (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
  - **Given** the fix is applied **When** the user selects each of `CHECKING`, `SAVINGS`, `CREDIT_CARD`, `CASH`, `INVESTMENT` from the dropdown **Then** the selected value is reflected in the text field display and passed correctly to `onSave`; selecting `CREDIT_CARD` disables the "Include in Safe to Spend" toggle
  - **NFRs:** Covers the spec constraint: "Account type must be one of: CHECKING, SAVINGS, CREDIT_CARD, CASH, INVESTMENT". Validate `CREDIT_CARD` side-effect.

---

## Section 5 — Definition of Done

- [ ] Reproduction test was failing before the fix and passes after
- [ ] All fix tasks are implemented and committed
- [ ] Acceptance criteria pass for every task
- [ ] No NFR benchmarks regressed
- [ ] No adjacent functionality broken (existing tests pass)
- [ ] Tapping "Type" on both Android and iOS (Compose Multiplatform) opens the dropdown with all five account types visible
- [ ] Selecting `CREDIT_CARD` still auto-disables the spending pool toggle
- [ ] Shadow spec reviewed — no gap identified; the spec's `AccountFormProps` interface is unchanged
- [ ] Bug report marked as resolved
