# Cross-Page Design QA

**Status:** Reviewed  
**Scope:** Home, Calendar, Accounts, Me / Settings, Sandbox mode, shared sheets

## Shared passes

- All previews use mobile-first, single-column vertical composition.
- Actual app/sheet surfaces use consistent phone-frame treatment:
  - `--p-phone-max: 24rem`
  - `--p-radius-device: 2rem`
  - `--p-touch-target: 3rem`
  - full border + shadow
- Surrounding preview document uses normal theme background; no gray stage treatment.
- Light/dark semantic tokens exist in every preview.
- Preview controls expose theme toggle and `aria-pressed` state.
- CSS variable references resolve; braces balance.
- Preview scripts pass syntax validation.
- No fixed page/card/row widths; max device width remains fluid.
- Interactive rows/actions use explicit borders or surfaces; native button border artifacts removed.
- Section heading-to-content spacing uses existing spacing tokens.
- Loading, empty, error, negative/warning states remain scoped to affected content.
- Currency samples communicate amount meaning with signs/text in addition to tone.

## Page coverage

| Surface | Default | Loading | Empty | Error | Negative/warning | Theme toggle |
|---|---:|---:|---:|---:|---:|---:|
| Home | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Calendar | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Accounts | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Me / Settings | ✓ | ✓ | ✓ | ✓ | — | ✓ |
| Sandbox mode | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Shared sheets | ✓ | — | — | ✓ | — | ✓ |

## Accessibility review

- Page and sheet headings identify current surface.
- Buttons and picker rows expose action meaning.
- Selected state is represented semantically, not color-only.
- Transaction/account/card labels include relevant identity and amount context.
- Errors and success feedback use text/live-region treatments.
- Destructive actions are separated and confirmation-owned.
- Touch target token is consistently 48dp-equivalent in preview.

## Responsive review

- Phone frame remains fluid below max width.
- Preview outer content remains bounded without fixed page width.
- Calendar uses seven flexible columns.
- Transaction/account/card leading content yields before trailing amount/actions.
- Sheet bodies can scroll; forms remain single-column and action footers remain reachable.
- No preview uses multi-column galleries.

## Decisions normalized during review

- Home preview phone radius token renamed to shared `--p-radius-device`.
- Home outer background now directly uses `bgPrimary`, matching all other previews.
- Overdue status uses full high-contrast badge treatment.
- Shared sheet state preview receives outer token padding so labels/panels do not touch frame edges.

## Remaining Build work

- Implement shared sheet shell and migrate feature sheets.
- Implement exact Home-to-Calendar selected-date handoff.
- Add dark semantic source palette/system theme selection.
- Migrate badge/error/info tokens to canonical semantic values.
- Replace local raw dimensions in existing Compose components with design tokens.
- Preserve page/mode boundaries: Sandbox remains Home mode; BudgetScreen and RecurringScreen remain unused.
