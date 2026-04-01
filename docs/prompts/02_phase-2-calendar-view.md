# Phase 2: Calendar View

> Monthly grid visualization of financial activities wired to Phase 1 ViewModels

**Features:** Calendar View, Safe to Spend Header
**Shadow Spec:** specs/shadow/Calendar.shadow.md

---

## Group A: Domain Models

- [x] Define `CalendarDay` data class with `date: LocalDate`, `isCurrentMonth`, `isToday`, `isSelected`, `dailySummary: DaySummary?` in `features/calendar/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Define `DaySummary` data class with `totalIncome`, `totalExpenses`, `netAmount`, `transactionCount`, `hasPending`, `hasOverdue`, `hasConfirmed` fields (all monetary values in cents as `Long`) in `features/calendar/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Define `CalendarMonth` data class with `yearMonth: YearMonth`, `days: List<CalendarDay>`, `transactionsByDate: Map<LocalDate, List<Transaction>>` in `features/calendar/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Define `CalendarEvent` data class and `CalendarEventType` enum (`PENDING_INCOME`, `PENDING_EXPENSE`, `CONFIRMED_INCOME`, `CONFIRMED_EXPENSE`, `OVERDUE`) in `features/calendar/domain/model/` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

---

## Group B: Use Cases

- [x] Implement `GetMonthTransactionsUseCase` — queries `ITransactionRepository.getByDateRange(firstOfMonth, lastOfMonth)` and returns `Flow<List<Transaction>>` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `CalculateDaySummaryUseCase` — groups a list of transactions by date, computes `DaySummary` per `LocalDate` (income sum, expense sum, net, hasPending, hasOverdue, hasConfirmed) (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `BuildCalendarMonthUseCase` — constructs a full `CalendarMonth` for a given `YearMonth`: pads leading/trailing days from adjacent months, sets `isCurrentMonth`, `isToday`, attaches `DaySummary` to each day (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Register calendar use case bindings in Koin `useCaseModule` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

---

## Group C: Presentation Layer

- [x] Define `CalendarUiState` data class: `currentMonth: YearMonth`, `selectedDate: LocalDate`, `calendarMonth: CalendarMonth?`, `selectedDayTransactions: List<Transaction>`, `safeToSpend: Long`, `isLoading: Boolean`, `error: String?` in `features/calendar/presentation/` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `CalendarViewModel` with: `loadMonth(yearMonth)`, `selectDate(date)`, `navigateMonth(direction: Int)` — wires `GetMonthTransactionsUseCase`, `BuildCalendarMonthUseCase`, `GetSafeToSpendUseCase`; emits `CalendarUiState` via `StateFlow` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Register `CalendarViewModel` in Koin `viewModelModule` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)

---

## Group D: UI Components (Compose)

- [x] Implement `SafeToSpendHeader` composable: displays `availableToSpend` formatted via `CurrencyUtils`, accent-light background, large bold amount, "Safe to Spend" label, loading shimmer state (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `MonthNavigationHeader` composable: month/year title, left/right arrow `IconButton`s, calls `onPreviousMonth` / `onNextMonth` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `CalendarDayCell` composable: date number, net amount formatted, state dot indicators (amber for pending, red for overdue, green checkmark for confirmed), grayed-out styling (40% alpha) for adjacent-month days, today highlight, selected highlight (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `CalendarGrid` composable: 7-column `LazyVerticalGrid` with day-of-week header row (Sun–Sat), renders `CalendarDayCell` for each `CalendarDay`, calls `onDateSelected` on tap (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `DayTransactionList` composable: scrollable column of transaction rows for the selected date, each row shows amount, category, status badge, account name; shows "No transactions" empty state; shows "View all X transactions" link when count > 20 (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Implement `CalendarScreen` composable: composes `SafeToSpendHeader` + `MonthNavigationHeader` + `CalendarGrid` + `DayTransactionList`; collects `CalendarUiState` from `CalendarViewModel`; handles swipe-left/right gestures for month navigation (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
- [x] Replace template `App()` content in `App.kt` with `CalendarScreen`, injecting `CalendarViewModel` via Koin `koinViewModel()` (Use Skill: kmp) (Reference: specs/shadow/Calendar.shadow.md)
