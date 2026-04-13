# Bug: Account Type Dropdown Does Not Open on Tap

**Date:** 2026-04-05  
**Feature:** Accounts  
**Severity:** Medium — blocks users from changing account type during creation/editing

---

## Description

When creating or editing an account, tapping the "Type" field does not open the dropdown menu to select an account type. The field appears interactive (shows a "v" trailing icon), but tapping it has no effect.

**Steps to reproduce:**
1. Open the Accounts screen
2. Tap the `+` button to open "Add Account"
3. Tap the "Type" field (shows "Checking" by default)
4. Observe: no dropdown appears

---

## Root Cause Analysis

Two compounding issues in `AccountFormSheet.kt`:

### Issue 1 — `clickable` on `OutlinedTextField` is swallowed

At line 109–112, a `clickable` modifier is applied to the `OutlinedTextField`:

```kotlin
modifier = Modifier
    .fillMaxWidth()
    .clickable { typeMenuExpanded = true },
```

`OutlinedTextField` internally consumes pointer events for focus and cursor handling. The `clickable` modifier placed on the text field is intercepted by the field's own click handling before it can set `typeMenuExpanded = true`. As a result, the state never changes and the menu never opens.

### Issue 2 — `DropdownMenu` is not anchored to the text field

At line 114–127, the `DropdownMenu` is placed as a sibling of the `OutlinedTextField` inside the `Column`, not inside a wrapping `Box` that contains both. `DropdownMenu` in Compose anchors to its **immediate parent layout bounds**. With the current structure the anchor is the `Column` rather than the text field, so even if the menu did open it would appear at an unexpected position (likely at the top of the column or off-screen).

---

## Affected Files

| File | Lines |
|------|-------|
| `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/features/accounts/presentation/components/AccountFormSheet.kt` | 104–127 |

---

## Proposed Solution

Replace the manual `OutlinedTextField` + `DropdownMenu` pair with Material3's `ExposedDropdownMenuBox` + `ExposedDropdownMenu` combo. This API was designed exactly for this pattern:

- `ExposedDropdownMenuBox` provides an `Modifier.menuAnchor()` that correctly anchors the dropdown to the text field bounds.
- It exposes an `expanded`/`onExpandedChange` callback that hooks into the text field's own click/focus events, bypassing the swallowed-click problem.
- `ExposedDropdownMenu` replaces the bare `DropdownMenu` and inherits the correct anchor automatically.

No logic changes needed — only the composable structure around the type selector needs to change.
