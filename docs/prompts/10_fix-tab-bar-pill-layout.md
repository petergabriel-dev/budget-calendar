# Fix: BcTabBarPill — Center Add Button Between Tabs

**Feature:** Design System — Tab Bar Layout Correction
**Shadow Spec:** `specs/shadow/DesignSystem.shadow.md`
**Design Spec Rule:** DS-007
**Component ID:** EZOJk (Tab Bar Pill)
**File:** `src/mobile/composeApp/src/commonMain/kotlin/com/petergabriel/budgetcalendar/core/designsystem/component/BcTabBarPill.kt`

---

## Context

The `BcTabBarPill` composable currently renders all tabs first, then appends the `+` add button at the far right end of the row. The design spec (EZOJk, DESIGN_SYSTEM.md Section 6.1) requires the add button to be centered between the first and second halves of the tabs:

**Current (wrong):** Home | Calendar | Accounts | Me | +
**Required (correct):** Home | Calendar | + | Accounts | Me

---

## Tasks

- [ ] Before implementing: read node `EZOJk` from `specs/DESIGN_SYSTEM.pen` using `mcp__pencil__batch_get` to confirm the exact Tab Bar Pill layout spec, then verify the current `BcTabBarPill.kt` implementation against it. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] Fix `BcTabBarPill.kt`: split the `tabs` list at `tabs.size / 2` when `onAddClick != null`. Render `tabs.take(splitIndex)` first, then the add button `Box`, then `tabs.drop(splitIndex)`. Remove the old single `tabs.forEach` loop and the trailing add button block, replacing them with the split rendering. Do not change any styling, sizing, or token usage. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)

- [ ] After implementing: call `mcp__pencil__batch_get` on node `EZOJk` again and diff the rendered spec against the updated code. Confirm the add button is visually centered between Calendar and Accounts tabs before marking done. (Use Skill: kmp) (Reference: specs/shadow/DesignSystem.shadow.md)
