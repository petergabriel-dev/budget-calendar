# Daily Velocity Display

> Home screen Budget hero shows dynamic `₱X / day · N days left` below the STS amount,
> computed from `daysRemainingInMonth` (today inclusive). Last day of month triggers a
> red warning state with ⚠ icon and tappable tooltip.

**Shadow spec:** `specs/shadow/Budget.shadow.md`
**Skill:** `kmp`

---

## Section 1 — Non-Functional Requirements

- **Performance:** `daysRemainingInMonth()` is a pure, synchronous, local computation — no DB access, no coroutine, no network call. Must complete in < 1ms.
- **Reliability:** Must produce correct results for all calendar months (28, 29, 30, 31 days). February on leap years must work. Uses `kotlinx-datetime` `LocalDate` arithmetic — no manual day-count arrays.
- **Correctness:** Today is always counted as a remaining day. `daysRemainingInMonth` range is `1..31`. It must never return 0 or a negative value.
- **Timezone:** Must use device local timezone (`TimeZone.currentSystemDefault()`), consistent with all other `DateUtils` operations.
- **Accessibility:** Last-day warning must not rely on color alone — the `⚠` AlertCircle icon is required alongside red text. Tooltip must be dismissible by tapping again.
- **Design tokens:** All colors, typography, and spacing must come from `BudgetCalendarTheme` — no raw hex, dp, or sp literals.

---

## Section 2 — Success Metrics

- `daysRemainingInMonth()` returns correct values for every day of every month (verified by unit tests covering all edge cases).
- Hardcoded `/ 30` divisor is fully eliminated from `HomeScreen.kt`.
- Daily rate row appears **below** the STS amount, not beside it.
- `· N days left` label renders alongside the daily rate in normal state.
- On the last day of the month: text is `colors.error`, AlertCircle icon is shown, row is tappable, tooltip appears/disappears on tap.
- On all other days: text and icon are `colors.textSecondary`, no tooltip trigger.
- When `availableToSpend <= 0`: daily rate row is not rendered.
- Pencil component `0yIOD` (Hero / Safe to Spend) is read before and after implementation to confirm visual fidelity.
- All existing `HeroSafeToSpend` callers compile without modification (default params handle backward compat during transition, then cleaned up in HomeScreen).

---

## Section 3 — Risks and Assumptions

### Assumptions

- `kotlinx-datetime` `LocalDate.lengthOfMonth()` (or equivalent — `month.length(isLeapYear)`) is available in the version already on the classpath. If not, use `LocalDate(year, month+1, 1).minus(DatePeriod(days=1)).dayOfMonth` pattern.
- `HeroSafeToSpend` has no other callers besides `HomeScreen.kt` (confirmed by grep before starting).
- The tooltip implementation uses Compose's `Popup` or an inline `Box` overlay — no third-party library needed.

### Risks

- **`LocalDate` month length API:** The exact function name for "days in month" varies across `kotlinx-datetime` versions. Verify the available API before writing the utility, and fall back to the `DatePeriod` arithmetic approach if needed.
- **Tooltip z-ordering:** A Compose `Popup` may clip at screen edges on some Android API levels. If layout clipping is observed, switch to an inline conditional `Box` that renders the tooltip text below the row.
- **`isLastDayOfMonth` computed at call site vs. inside component:** Computing it in `HomeScreen` keeps the component pure and testable. Ensure the value is recomputed on every recomposition (not cached in a `remember` that doesn't track date changes across midnight).

---

## Section 4 — Tasks

- [ ] Add `daysRemainingInMonth(today: LocalDate, tz: TimeZone): Int` to `DateUtils` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
  - **Given** any valid `LocalDate` **When** `daysRemainingInMonth` is called **Then** it returns `lastDayOfMonth - today.dayOfMonth + 1`, which equals 1 on the last day of the month and equals the total days in the month on the 1st
  - Use `cocoindex-code` to search `"DateUtils daysRemainingInMonth"` first to confirm the function doesn't already exist
  - Verify the available `kotlinx-datetime` API for getting the last day of a month (check `LocalDate`, `Month`, `DatePeriod` approaches) before writing
  - **NFRs:** Pure function, no coroutine, no DB, must handle 28/29/30/31-day months and leap years

- [ ] Write unit tests for `daysRemainingInMonth` covering all edge cases (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
  - **Given** the new utility **When** called for the 1st, mid-month, last day of a 28/29/30/31-day month **Then** all return correct values
  - Test cases must include: Jan 1 (→31), Jan 31 (→1), Feb 28 non-leap (→1), Feb 28 leap (→2), Feb 29 leap (→1), Mar 15 (→17), Dec 31 (→1)
  - Place tests in the existing `androidUnitTest` source set alongside other `DateUtils` tests (or create `DateUtilsTest` if it doesn't exist)
  - **NFRs:** See Section 1

- [ ] Update `HeroSafeToSpend` component signature and layout (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
  - **Given** the current `HeroSafeToSpend.kt` **When** updated **Then**:
    - Add params: `daysRemaining: Int? = null`, `isLastDayOfMonth: Boolean = false`
    - Remove the daily rate from the `Row` beside the STS amount
    - Add a new row below the STS amount that renders `₱X / day · N days left`
    - Normal state: `typography.bodyMedium`, `fontWeight = FontWeight.Light`, `color = colors.textSecondary`
    - Warning state (`isLastDayOfMonth == true`): `color = colors.error`, `AlertCircle` icon (16.dp, `colors.error`) leading the text, entire row is `clickable` toggling a `showTooltip` local state
    - Tooltip: inline `Box` or `Popup` rendering below the row — `bg = colors.bgDark`, `radius = 8.dp`, `12.sp`, `color = colors.textInverted`, text = `"You have 1 day left this month. This is your full Safe to Spend balance."`
    - Daily rate row is not rendered when `dailyRate == null`
  - Before implementing: call `mcp__pencil__batch_get` with node `0yIOD` from `specs/DESIGN_SYSTEM.pen` to read the authoritative visual spec
  - After implementing: re-read node `0yIOD` and diff against the code to confirm layout fidelity
  - **NFRs:** All design tokens from `BudgetCalendarTheme` — no raw hex/dp/sp

- [ ] Update `HomeScreen.kt` to pass `daysRemaining` and `isLastDayOfMonth` to `HeroSafeToSpend` (Use Skill: kmp) (Reference: specs/shadow/Budget.shadow.md)
  - **Given** the current `HomeScreen.kt` which passes `dailyRate = availableToSpend / 30` **When** updated **Then**:
    - Import `kotlinx.datetime.Clock`, `kotlinx.datetime.TimeZone`, `kotlinx.datetime.toLocalDateTime`
    - Compute `today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date`
    - Compute `daysRemaining = DateUtils.daysRemainingInMonth(today)`
    - Compute `isLastDayOfMonth = daysRemaining == 1`
    - Compute `dailyRate = if (availableToSpend > 0L) availableToSpend / daysRemaining else null`
    - Pass all three to `HeroSafeToSpend`
    - Remove the old `availableToSpend / 30` expression entirely
  - Use `cocoindex-code` search `"HeroSafeToSpend dailyRate HomeScreen"` to locate all call sites before editing
  - **NFRs:** `today` must not be cached in a `remember` block without a key that changes at midnight — compute inline or use `remember { }` with `currentTimeMillis` as key if needed to avoid stale date across midnight

---

## Section 5 — Definition of Done

- [ ] All tasks above are implemented and committed
- [ ] `daysRemainingInMonth` unit tests pass for all edge cases (28/29/30/31-day months, 1st, mid, last day)
- [ ] Hardcoded `/ 30` divisor is gone from the codebase (`grep -r "/ 30"` in budget/home files returns no results)
- [ ] Daily rate row renders below (not beside) the STS amount
- [ ] `· N days left` label is visible in normal state
- [ ] Last-day warning: red text, ⚠ icon, tappable tooltip — confirmed via manual test on a device/emulator with date set to last day of a month
- [ ] Pencil confirmation loop completed for `HeroSafeToSpend` (read before + read after)
- [ ] No regressions in existing `CalculateSafeToSpendUseCase` tests
- [ ] CI build passes (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] Shadow spec reflects final implementation (update if any deviation occurred)
