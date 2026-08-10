# Color Tokens

**Status:** Proposed
**Scope:** Light and dark semantic palettes

## Token rule

Components consume semantic `BcColors` names. Components never select light/dark hex values directly.

## Semantic token table

| Token | Light | Dark | Meaning |
|---|---|---|---|
| `bgDark` | `#000000` | `#FAFAFA` | Highest-contrast action/selected surface |
| `bgMuted` | `#27272A` | `#27272A` | Strong muted background |
| `bgPrimary` | `#FFFFFF` | `#09090B` | Page background |
| `bgSurface` | `#F4F4F5` | `#18181B` | Raised/control surface |
| `textPrimary` | `#000000` | `#FAFAFA` | Primary content |
| `textSecondary` | `#71717A` | `#A1A1AA` | Supporting content |
| `textTertiary` | `#A1A1AA` | `#71717A` | Low-emphasis content |
| `textInverted` | `#FFFFFF` | `#09090B` | Content on `bgDark` |
| `textDisabled` | `#D4D4D8` | `#52525B` | Disabled content |
| `borderStrong` | `#E4E4E7` | `#3F3F46` | Visible separators |
| `focusRing` | `#000000` | `#FAFAFA` | Keyboard/focus indicator |
| `borderSubtle` | `#F4F4F5` | `#27272A` | Low-emphasis separators |
| `colorSuccess` | `#22C55E` | `#4ADE80` | Success icon/accent |
| `colorSuccessText` | `#2E7D32` | `#86EFAC` | Success text |
| `colorSuccessBg` | `#DCFCE7` | `#14532D` | Success surface |
| `colorError` | `#EF4444` | `#F87171` | Error icon/accent |
| `colorErrorText` | `#C62828` | `#FCA5A5` | Error text |
| `colorErrorBg` | `#FEE2E2` | `#7F1D1D` | Error surface |
| `colorWarning` | `#F59E0B` | `#FBBF24` | Warning icon/accent |
| `colorWarningText` | `#92400E` | `#FDE68A` | Warning text |
| `colorWarningBg` | `#FEF3C7` | `#78350F` | Warning surface |
| `colorInfo` | `#3B82F6` | `#60A5FA` | Informational icon/accent |
| `colorInfoText` | `#1D4ED8` | `#BFDBFE` | Informational text |
| `colorInfoBg` | `#DBEAFE` | `#1E3A8A` | Informational surface |

## Usage rules

- `bgPrimary` and `bgSurface` define page/control contrast in both themes.
- `bgDark` is semantic high-contrast action surface, not fixed black.
- `textInverted` must remain readable on `bgDark`.
- Use `color*Text` for text on page/surface backgrounds when available.
- Use `colorWarningText` and `colorInfoText` for warning/info text.
- Use `color*Bg` for status containers; pair with readable status text.
- Use `focusRing` for interactive focus; do not use border tokens as focus treatment.
- Use `textTertiary` only for non-essential metadata; never for required instructions or actions.
- Do not use `colorSuccess`, `colorError`, or accent tokens as the only status signal.
- Validate normal text, large text, focus boundaries, badges, and disabled states in both themes.

## Component mapping

Status badges use accent backgrounds with dedicated text tokens:

| Badge variant | Container | Content |
|---|---|---|
| Success | `colorSuccessBg` | `colorSuccessText` |
| Warning | `colorWarningBg` | `colorWarningText` |
| Error | `colorErrorBg` | `colorErrorText` |
| Info | `colorInfoBg` | `colorInfoText` |

Overdue badge remains high-contrast `bgDark` + `textInverted`. Pending badge uses `bgSurface`, `textSecondary`, and `borderStrong`.

Implementation adds `colorWarningText`, `colorInfoText`, and `focusRing` to `BcColors`. Existing component APIs remain unchanged.

## Theme selection

- `BudgetCalendarTheme` selects light/dark palette from system appearance.
- No user theme preference in this pass.
- Existing component APIs remain unchanged.
