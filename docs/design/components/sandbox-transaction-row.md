# Sandbox Transaction Row

**Status:** Implemented
**Surface:** Home page, Sandbox mode  
**Layer:** Feature component

## Purpose

Show one planned transaction with type meaning and safe mutation actions. Keep promotion and removal explicit and separate from row content.

## Contract

```kotlin
@Composable
fun SandboxTransactionRow(
    transaction: SandboxTransaction,
    onPromote: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
)
```

Caller owns transaction loading and mutation results. Component may own local confirmation visibility only.

## Anatomy

```text
[Expense]  Groceries                         −₱2,500.00
           Weekend trip · Jun 22

                                  Promote   Remove
```

- Type badge always visible: Income, Expense, Transfer.
- Category/name fallback: `Uncategorized`.
- Optional description and date remain secondary.
- Signed amount uses type tone plus sign/text meaning.
- Promoted item hides Promote and exposes promoted state.
- Remove is destructive semantic.

## Behavior

- Promote opens confirmation before callback.
- Confirmation states real-data consequence: `This adds the transaction to real data and removes it from sandbox.`
- Remove confirms when required, then emits id.
- Row itself is not a single navigation action; controls remain individually reachable.

## Accessibility

- Row content label includes type, name, description, date, signed amount, and promoted state.
- Promote action announces consequence before confirmation.
- Remove action is clearly destructive.
- Amount meaning does not rely on color.
- Every action meets `spacing12` (48dp).

## Responsive

- Mobile-first, full width.
- Leading content ellipsizes before amount is clipped.
- Actions remain visible; stack action controls when width is tight.
- No fixed row width.

## Theme

Use `bgSurface`, semantic type/amount/destructive tokens, border, focus, radius, spacing, and typography tokens in light/dark themes.
