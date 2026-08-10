# Me / Settings Page

**Status:** Proposed  
**Surface:** Me tab  
**Layer:** Page composition

## Purpose

Give users one focused place to configure Safe to Spend notification behavior. Keep Me tab minimal until additional profile/settings features exist.

## Page shell

```text
Scrollable content
    Header
    Notification Preferences surface

App shell bottom navigation remains outside page content.
```

- Bottom tab label: `Me`.
- Page/header title: `Settings`.
- Background: `bgPrimary`.
- Header action: `BcNotificationBell`.
- Use existing page insets and vertical spacing tokens.
- Do not invent profile/account sections without product data.
- Notification center remains a global sheet opened by the bell.

## Composition

1. `BcHeader("Settings")`.
2. `NotificationPreferencesCard`.
3. Full-width numeric threshold input.
4. Helper text.
5. Full-width primary Save action.
6. Scoped validation/error/success feedback.

## Notification Preferences

- Surface: `bgSurface` with existing radius and token padding.
- Section title: `Notification Preferences`.
- Field label: `STS Low Alert Threshold (%)`.
- Numeric input accepts whole-number percentages only.
- Helper text: `Valid range: 1 to 100. Default is 15.`
- Save button is full width and primary.
- Threshold controls Safe to Spend low-alert notification generation; it does not change Safe to Spend calculation.

## Interaction

- Input change clears previous validation and success feedback.
- Save validates before submission.
- Valid range is inclusive: 1–100.
- Save success shows `Threshold saved` in a persistent inline live region.
- Save failure stays inside the card and preserves entered value.
- Saving disables the action and changes label to `Saving...`.
- Bell opens global NotificationCenterSheet; settings page does not own notification list state.

## State matrix

| Area | Loading | Empty | Error |
|---|---|---|---|
| User settings | Preserve card shell with input skeleton | Use default threshold 15 when no saved settings exist | Inline load error with retry; do not show unknown value as saved |
| Threshold input | Disabled skeleton/placeholder | Default `15` | Preserve last valid value when available |
| Validation | N/A | N/A | `Please enter a threshold between 1 and 100` below field |
| Save | `Saving...`, disabled | Enabled when value valid | Inline save error; value remains editable |
| Success | N/A | N/A | `Threshold saved`, then clears on next edit |

## Accessibility

- Header exposes Settings page identity and notification action.
- Input label is programmatically associated with numeric field.
- Input exposes percentage unit and valid range in helper/error text.
- Invalid state is announced and remains adjacent to field.
- Save action exposes disabled/busy state while saving.
- Success/error feedback uses a live region and does not rely on color alone.
- All controls meet `spacing12` (48dp).
- Preserve visible focus and keyboard focus order.

## Responsive

- Mobile-first, single-column layout.
- Card uses full available width; no fixed page or input width.
- Numeric field and Save action remain full width.
- Helper/error text wraps naturally.
- Wider layouts may use bounded content width but do not create settings galleries.

## Theme and token rules

- Use `bgPrimary`, `bgSurface`, semantic text/error/success tokens, typography, spacing, radius, and focus tokens.
- Success text must include `Threshold saved`, not color alone.
- Input border/error treatment must remain clear in both themes.
- No user theme preference in this pass; system-driven theme remains deferred to source implementation.

## Known implementation gaps

- Current screen uses a snackbar for load/save errors in surrounding flows; persistent settings failures should remain visible inside the card.
- Current input label uses abbreviated `STS`; Build should decide whether to preserve exact copy or expand to `Safe to Spend` while maintaining product language.
- Settings currently contains one preference only; keep page intentionally sparse until new requirements exist.
