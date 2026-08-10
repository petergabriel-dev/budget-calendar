# End-of-Month Projection Pill

**Status:** Implemented
**Surface:** Calendar page  
**Layer:** Feature component

## Purpose

Show projected end-of-month balance as supporting Calendar context. Keep it visible without competing with selected-day transaction detail.

## Contract

```kotlin
@Composable
fun EndOfMonthProjectionPill(
    projectionAmount: Long?,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

Amount is cents. Component formats currency and renders local loading/error treatment. Projection calculation remains in the ViewModel/use case.

## Anatomy

```text
End of Month Projection                         ₱12,480.00
```

- Full-width non-interactive surface.
- Label uses secondary text.
- Amount uses bold title hierarchy.
- Negative/zero amount uses `colorErrorText` plus explicit accessible warning text.

## States

### Loading

Preserve pill shape and label. Replace amount with token skeleton. Expose progress semantics.

### Empty

Show neutral placeholder such as `Not available`. Do not show zero as fake loaded data.

### Error

Show concise inline error and retry only when supplied. Grid and selected-day section remain usable.

### Content

Show formatted amount with no sign ambiguity. Use caller-provided currency rules.

## Accessibility

Label and amount form one metric description. Negative/zero meaning is announced as text. Retry identifies projection operation. Component is not a button unless an explicit action is supplied.

## Responsive

- Mobile-first, full available width.
- Label may wrap before amount is clipped.
- Amount remains visible at narrow widths.
- No fixed width.

## Theme

Use `bgSurface`, `textSecondary`, `textPrimary`, `colorErrorText`, radius, spacing, typography, and skeleton tokens. Validate both themes.
