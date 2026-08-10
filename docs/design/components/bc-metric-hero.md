# BcMetricHero

**Status:** Proposed
**Surface:** Home metrics
**Layer:** Core design-system foundation

## Purpose

Provide shared visual and semantic foundation for large currency metrics. Replace duplicated hero layout across `HeroSafeToSpend`, `HeroProjectedSpend`, and Available Credit.

Feature wrappers keep domain wording and calculations. `BcMetricHero` owns hierarchy, amount presentation, supporting text, warning treatment, loading, and accessibility.

## Contract

```kotlin
enum class BcMetricHeroHierarchy {
    Primary,
    Secondary,
}

enum class BcMetricHeroTone {
    Neutral,
    Positive,
    Warning,
    Negative,
}

@Composable
fun BcMetricHero(
    label: String,
    amount: Long,
    hierarchy: BcMetricHeroHierarchy = BcMetricHeroHierarchy.Primary,
    tone: BcMetricHeroTone = BcMetricHeroTone.Neutral,
    supportingText: String? = null,
    warningMessage: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onWarningClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

Amount is cents. Component formats currency through existing currency rules. Component performs no budget, credit, date, or projection calculations.

## Hierarchy

### Primary

- `displayMedium` amount.
- Used for Safe to Spend and Projected Spend.
- Receives strongest visual priority in page flow.

### Secondary

- `title` amount.
- Used for Available Credit.
- Same label/supporting structure with reduced visual weight.
- Must not compete with primary Safe to Spend metric.

## Tone

| Tone | Amount treatment | Use |
|---|---|---|
| Neutral | `textPrimary` | Normal metric state |
| Positive | `colorSuccessText` or approved success semantic | Explicit positive outcome |
| Warning | `colorWarningText` or approved warning semantic | Last-day/context warning |
| Negative | `colorErrorText` or approved error semantic | Zero/negative projected result |

Do not use positive tone for Safe to Spend by default. Neutral remains normal state.

## Supporting content

- `supportingText` uses `bodyMedium` and secondary color.
- Daily-rate context format: `₱X.XX / day · N days left`.
- Omit supporting line when no context exists.
- `warningMessage` appears below supporting text when warning state is active.
- Safe to Spend last-day treatment uses one warning-colored supporting line only; no icon or tooltip.
- Any optional warning action must expose action meaning; no color-only warning.

## Variants

### Safe to Spend

```text
label = SAFE TO SPEND
hierarchy = Primary
tone = Neutral
supportingText = daily rate + days remaining
```

Last day keeps the amount Neutral. Daily-rate supporting line uses Warning tone: `₱X.XX / day · 1 day left`. No icon or tooltip.

### Available Credit

```text
label = AVAILABLE CREDIT
hierarchy = Secondary
tone = Neutral
supportingText = optional
```

Hide when no configured available credit exists. This is secondary data, not an error when absent.

### Projected Spend

```text
label = PROJECTED SPEND
hierarchy = Primary
tone = Negative when amount <= 0, otherwise Neutral
supportingText = optional daily rate
```

Negative/zero state uses Negative amount tone and explicit accessible warning text. Warning icon is optional, not required by base component.

## States

### Loading

- Preserve label and layout.
- Replace amount/supporting content with token-based skeletons.
- Expose progress semantics.
- Do not show zero as loaded value.

### Error

- Keep label visible.
- Show concise inline error.
- Show retry only when `onRetry` exists.
- Do not show stale amount as current without caller-provided stale-data treatment.

### Content

- Render amount, hierarchy, tone, and supporting context.

## State precedence

```text
loading > error > content
```

- Loading renders skeletons only.
- Error renders label, error message, and retry when supplied.
- Content renders amount and supporting context.
- Never present zero as a fake loaded value.

## Accessibility

- Expose label, formatted amount, supporting text, tone meaning, and warning/error text as one coherent metric description.
- Amount meaning must not rely on color.
- Loading exposes progress state.
- Error exposes failure and retry action.
- Warning click target meets `spacing12` (48dp).
- Do not make entire metric clickable unless caller supplies an explicit action.

## Responsive

- Mobile-first, full available width.
- Amount uses hierarchy typography without fixed width.
- Long labels ellipsize or wrap before amount is clipped.
- Supporting text wraps naturally.
- Secondary metrics remain stacked below primary metrics on narrow screens.
- Wider layouts may use bounded parent width; do not create fixed metric widths.

## Theme

- Add `BcDarkColors` using same semantic token names as light theme.
- Select dark mode from system theme.
- Defer user theme preference.
- Validate neutral, warning, negative, error, and disabled contrast in both themes.
- Keep `BcMetricHero` API unchanged across themes.

## Migration

- Refactor `HeroSafeToSpend` into domain wrapper over `BcMetricHero`.
- Refactor `HeroProjectedSpend` into domain wrapper over `BcMetricHero`.
- Render Available Credit through `BcMetricHero` with Secondary hierarchy.
- Keep domain wrappers responsible for daily-rate calculations, days-remaining rules, and variant labels.
