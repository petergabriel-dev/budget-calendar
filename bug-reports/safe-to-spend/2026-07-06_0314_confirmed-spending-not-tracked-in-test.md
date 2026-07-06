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

## Revisit

Investigate why `CalculateSafeToSpendUseCase` reports `confirmedSpending = 0` after `CreateTransactionRequest(... status = CONFIRMED ...)` in `BudgetUseCasesTest.kt`.
