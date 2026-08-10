# Notification Threshold Setting

**Status:** Proposed  
**Surface:** Me / Settings page  
**Layer:** Feature component

## Purpose

Edit Safe to Spend low-alert threshold as one validated, accessible setting. Keep persistence and notification generation outside component.

## Contract

```kotlin
@Composable
fun NotificationThresholdSetting(
    value: String,
    isLoading: Boolean,
    isSaving: Boolean,
    validationError: String?,
    saveError: String?,
    didSave: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

Value is a whole-number percentage string while editing. ViewModel owns parsing, validation, persistence, and default selection.

## Anatomy

```text
Notification Preferences

STS Low Alert Threshold (%)
[ 15                                  ]
Valid range: 1 to 100. Default is 15.

[ Save ]

optional validation/error/success feedback
```

- Surface uses `bgSurface`, existing radius, and token padding.
- Numeric keyboard hint on mobile.
- Save remains full width.
- Preserve entered value after validation or save failure.

## Validation

- Accept digits only.
- Valid range: 1–100 inclusive.
- Blank, non-numeric, zero, or out-of-range values show: `Please enter a threshold between 1 and 100`.
- Validation appears adjacent to input and is announced.

## States

### Loading

Keep card structure. Show input skeleton/disabled field. Do not imply a saved value until settings load completes.

### Editing

Input is editable. Any new edit clears previous validation and success feedback.

### Saving

Disable Save and expose `Saving...`. Keep field value visible.

### Save error

Show concise inline error. Preserve value and re-enable Save.

### Saved

Show `Threshold saved` in a live region. Keep value editable.

## Accessibility

- Label is associated with input.
- Input exposes percentage unit, numeric keyboard expectation, and range helper.
- Error uses invalid semantics and identifies field.
- Save exposes disabled/busy state while saving.
- Success is announced as text; color is supplemental.
- Minimum touch target: `spacing12` (48dp).

## Responsive

- Full available width.
- No fixed input width.
- Helper and error text wrap naturally.
- Save remains full width.

## Theme

Use semantic surface, text, border, error, success, focus, spacing, radius, and typography tokens in light and dark themes.
