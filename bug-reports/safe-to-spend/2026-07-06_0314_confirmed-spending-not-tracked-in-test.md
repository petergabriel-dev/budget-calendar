# Bug: Confirmed Spending Not Tracked in Safe-to-Spend Test

**Date:** 2026-07-06
**Feature:** Budget / Safe to Spend
**Detected during:** Sandbox cleanup task 1 verification

---

## Symptom

`cd src/mobile && ./gradlew :composeApp:assembleDebug :composeApp:allTests` failed at:

```text
BudgetUseCasesTest.calculateSafeToSpend_confirmedExpenseDoesNotIncreaseSTS
java.lang.AssertionError: Confirmed spending should be tracked expected:<7500> but was:<0>
```

`assembleDebug` completed before the test failure.

## Notes

Sandbox cleanup deleted only dead sandbox UI/usecase files. Grep verified no remaining refs to deleted sandbox symbols and no `isSandbox = true` writer. Failure appears outside sandbox deletion scope.

## Status

Resolved 2026-07-06.

## Resolution

Root cause was test date rot, not sandbox cleanup. `CalculateSafeToSpendUseCase` reports `confirmedSpending` only inside wall-clock `DateUtils.currentMonthBounds()`, while `BudgetUseCasesTest.calculateSafeToSpend_confirmedExpenseDoesNotIncreaseSTS` used hardcoded `localDateMillis(2026, 4, 1)` dates. Once current month moved past April 2026, confirmed spending filtered to 0.

Fix: `BudgetUseCasesTest.kt` now dates those confirmed expense rows with `DateUtils.currentMonthBounds().first`. Trap documented in `docs/engineering/traps.md`.

Commits:
- `8dfa84b` Fix time-dependent confirmed spending test
- `fa1b945` Document STS current-month test trap
