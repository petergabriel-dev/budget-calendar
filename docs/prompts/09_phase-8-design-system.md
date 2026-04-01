# Phase 8: Design System Implementation

> Implements the 62-component design system as Compose Multiplatform theme infrastructure and reusable component library.

**Shadow Spec:** `specs/shadow/DesignSystem.shadow.md`
**Design Reference:** `specs/DESIGN_SYSTEM.md`

---

## Phase 8A: Theme Foundation

- [ ] **Search existing theme code**: Use `cocoindex-code` to search for `"Compose theme setup"`, `"Color definitions"`, `"Typography"`, and `"MaterialTheme"` to understand what theme infrastructure already exists before writing new code. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Download and bundle font assets**: Download Outfit (400, 500, 600, 700, 800, 900) and Inter (400, 500, 600) `.ttf` files from Google Fonts. Place them in `composeApp/src/commonMain/composeResources/font/`. Verify they load via `Res.font`. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `Color.kt`**: Define `BcColors` data class with all 19 design token colors from DESIGN_SYSTEM.md Section 2.1 (`bgDark`, `bgMuted`, `bgPrimary`, `bgSurface`, `textPrimary`, `textSecondary`, `textTertiary`, `textInverted`, `textDisabled`, `borderStrong`, `borderSubtle`, `colorSuccess`, `colorSuccessBg`, `colorError`, `colorErrorBg`, `colorWarning`, `colorWarningBg`, `colorInfo`, `colorInfoBg`). Create `LocalBcColors` CompositionLocal. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `Typography.kt`**: Define `BcTypography` data class with all 12 text styles from Section 3.2 (`displayLarge` through `calendarAmount`). Map Outfit and Inter font families using `Font(Res.font.*)`. Create `LocalBcTypography` CompositionLocal. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `Spacing.kt`**: Define `BcSpacing` data class with 9 spacing tokens from Section 4.1 (`spacing1` through `spacing12`). Create `LocalBcSpacing` CompositionLocal. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `Radius.kt`**: Define `BcRadius` data class with 6 radius tokens from Section 4.2 (`sm`, `md`, `lg`, `xl`, `xxl`, `full`). Create `LocalBcRadius` CompositionLocal. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `Shadow.kt`**: Define `BcShadows` data class with 4 shadow tokens from Section 4.3 (`sm`, `md`, `lg`, `xl`). Create `LocalBcShadows` CompositionLocal. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BudgetCalendarTheme.kt`**: Implement the top-level theme composable that provides all CompositionLocals (`BcColors`, `BcTypography`, `BcSpacing`, `BcRadius`, `BcShadows`) and wraps `MaterialTheme` with mapped color scheme and typography. Add companion object accessors (`BudgetCalendarTheme.colors`, `.typography`, `.spacing`, `.radius`, `.shadows`). (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Wire theme into App entry point**: Search for existing `App.kt` or `MainViewController.kt` entry points using `cocoindex-code`. Wrap the app root content in `BudgetCalendarTheme { }`. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

---

## Phase 8B: Core Reusable Components — Buttons & Badges

- [ ] **Create `BcButton.kt`**: Implement unified button composable supporting 6 variants via `ButtonVariant` enum: Primary (`mbxe4`), Outline (`xR72O`), Ghost (`MO989`), Destructive (`Ifqk0`), Primary Icon Leading (`Bdc8O`), Outline Icon Trailing (`m39bO`). All 48px height, pill radius, proper token usage. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcBadge.kt`**: Implement unified badge composable supporting 6 variants via `BadgeVariant` enum: Pending (`CVw4C`), Overdue (`ywlyv`), Success (`bHBso`), Warning (`pRGld`), Error (`b4SK9`), Info (`PkcO9`). All 12px radius, 11pt uppercase text. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

---

## Phase 8C: Core Reusable Components — Form Controls

- [ ] **Create `BcInputGroup.kt`**: Implement input field with label matching component `c9XFF`. 48px height, 12px radius, 1.5px border, Inter font. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcTextareaGroup.kt`**: Implement multi-line input with label matching component `auccy`. 96px height, Inter font. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcCheckboxRow.kt`**: Implement checkbox with label matching component `4SRSD`. 24x24px checkbox, 8px radius, Inter font. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcToggleSwitch.kt`**: Implement toggle switch with label matching component `tA0BH`. 44x24px track, 20x20px thumb. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcRadioGroup.kt`**: Implement radio group and radio item matching components `iN2jx` and `VWxgC`. 24x24px radio circles, 12px gap. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcSelectGroup.kt`**: Implement dropdown select with label matching component `8G9pp`. 48px height trigger, space-between layout. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcNumberInput.kt`**: Implement numeric input with label matching component `qpBrt`. 200px default width. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `LargeAmountInput.kt`**: Implement hero amount entry matching component `L2zpZ`. 72pt Outfit font, centered vertical layout. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

---

## Phase 8D: Core Reusable Components — Layout & Navigation

- [ ] **Create `BcHeader.kt`**: Implement page header matching component `CvyNk`. 40pt Outfit title, 44x44px icon button. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcSectionHeader.kt`**: Implement section header matching component `O3ux1`. 24pt Outfit title, optional action text. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcTabBarPill.kt`**: Implement bottom navigation matching component `EZOJk`. 62px height, pill shape, 56x56px add button, space-around layout. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcSearchBar.kt`**: Implement search input matching component `HVMad`. 48px height, pill shape, lucide search icon. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcSegmentedControl.kt`**: Implement toggle control matching component `mTtvR`. 48px height, pill shape, active/inactive segments. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcTabsVertical.kt`**: Implement vertical tabs matching component `hpkIJ`. 200px width, 44px tab height. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcBreadcrumbs.kt`**: Implement breadcrumb navigation matching component `7NUcN`. Horizontal, 8px gap, Inter font. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

---

## Phase 8E: Feature Components — Calendar, Transaction, Account, Metrics

- [ ] **Create `DayWrapperActive.kt` and `DayWrapperDefault.kt`**: Implement calendar day cells matching components `AbKW9` and `sqaBa`. 32pt Outfit day text, 6x6 dot indicator, 12px radius. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `TransactionItem.kt`**: Implement transaction row matching component `Ks9uf`. Horizontal layout, 24x24 status indicator, 16pt title, 12pt meta, 20pt amount. Include `TransactionStateStyle` visual mapping. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `ScheduleCard.kt`**: Implement schedule card matching component `0N6DT`. Surface background, 16px radius, 4px accent bar, 24pt date. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `TransactionListMobile.kt`**: Implement mobile transaction list matching component `S7FdH`. Vertical layout, 8dp gap, 12px radius cards. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `AccountCardDark.kt` and `AccountCardLight.kt`**: Implement account cards matching components `XOueE` and `iDE7I`. 180px fixed width, 20px radius, badge + amount + description layout. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `HeroSafeToSpend.kt`**: Implement Safe to Spend hero matching component `0yIOD`. 56pt primary amount, 24pt daily rate, Outfit font. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `HeroNetWorth.kt`**: Implement Net Worth hero matching component `UexMB`. 72pt amount, Outfit font, -3px letter spacing. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcStatCard.kt`**: Implement stat card matching component `X1K6P`. 180px width, 20px radius, label + value + trend layout. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

---

## Phase 8F: Feedback, List, Sandbox & Utility Components

- [ ] **Create `BcDialog.kt`**: Implement modal dialog matching component `TFM6d`. 400px width, 20px radius, 24px padding. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcConfirmDialog.kt`**: Implement confirm dialog matching component `0JGwC`. 380px width, 56x56 icon, error-bg icon background. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcAlert.kt`**: Implement unified alert composable supporting 4 variants: Success (`tCFvy`), Warning (`GdTRQ`), Error (`20TKu`), Info (`9teyP`). 12px radius, lucide icons. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcToast.kt`**: Implement toast notification matching component `TzE0w`. 320px width, dark background, 12px radius. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcTooltip.kt`**: Implement tooltip matching component `8CxCm`. Dark background, 8px radius, 12pt text. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcListContainer.kt` and `BcListItem.kt`**: Implement list components matching `mhCFr` and `rJTFv`. 48px item height, 8px radius, 12px gap, chevron indicator. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcAccordionItem.kt`**: Implement accordion item matching component `JxzdM`. 16px radius, chevron-right icon, expandable content. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `SimulationForm.kt` and `ResultsList.kt`**: Implement sandbox components matching `Ksdjt` and `qE6i6`. 16px radius, 24px padding, vertical layout with dividers. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcProgressBar.kt`**: Implement progress bar matching component `WdLZn`. 8px track height, 4px radius, label + value. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcDivider.kt`**: Implement horizontal and vertical dividers matching `8duML` and `4ftwe`. 1px, border-strong color. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcSkeleton.kt`**: Implement skeleton loading matching component `SNgxx`. 40x40 avatar, text lines, surface color. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Create `BcPagination.kt`**: Implement pagination matching component `wCzH5`. 36px buttons, 8px gap, active/inactive styles. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

---

## Phase 8G: Integration — Apply Theme to Existing Screens

- [ ] **Search existing screens**: Use `cocoindex-code` to search for `"existing Compose screens"`, `"AccountScreen"`, `"CalendarScreen"`, `"TransactionScreen"`, `"SandboxScreen"`, `"BudgetScreen"` to inventory all screens that need theme migration. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Migrate existing screens to BudgetCalendarTheme**: Replace all raw hex colors, inline TextStyles, raw dp values, and Material3 defaults with `BudgetCalendarTheme.colors.*`, `.typography.*`, `.spacing.*`, and `.radius.*` tokens. Replace any inline UI components with their `Bc*` design system equivalents. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Build the Main Dashboard screen layout**: Compose the dashboard from Section 6.1 using design system components: `BcHeader` → `BcSearchBar` → `HeroSafeToSpend` → `BcSegmentedControl` → Calendar Section → Schedule Section → `BcTabBarPill`. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

---

## Phase 8H: Verification

- [ ] **Build and run on Android**: Run `./gradlew :composeApp:assembleDebug` to verify all components compile. Install on emulator and visually verify theme tokens render correctly — check font families (Outfit/Inter), colors, spacing, and radius against DESIGN_SYSTEM.md. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] **Build and run on iOS**: Open Xcode project, build for iOS simulator. Verify fonts load correctly and components render consistently with Android. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)
