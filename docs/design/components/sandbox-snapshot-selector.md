# Sandbox Snapshot Selector

**Status:** Implemented
**Surface:** Home page, Sandbox mode  
**Layer:** Feature component

## Purpose

Expose active sandbox snapshot and open snapshot management without consuming page space with a full list.

## Contract

```kotlin
@Composable
fun SandboxSnapshotSelector(
    activeSnapshot: SandboxSnapshot?,
    hasSnapshots: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Snapshot creation, selection, deletion, and persistence remain sheet/ViewModel-owned.

## Visual rules

- Full-width pill.
- Active name or `No Snapshot Selected`.
- `bgDark` + `textInverted` treatment.
- Existing full radius, spacing, and touch-target tokens.
- Supporting icon is optional; accessible action label is required.
- Loading preserves pill shape with token skeleton.

## Behavior

Tap opens `SnapshotSelectorSheet`. Component does not mutate snapshot state.

## Accessibility

- Active snapshot name included in action label.
- Empty state announces `No Snapshot Selected`.
- Loading exposes progress semantics.
- Error identifies snapshot loading problem and retry/select action.
- Minimum touch target: `spacing12` (48dp).

## Responsive

- Full available width.
- Long names ellipsize before selector affordance disappears.
- No fixed width.

## Theme

Use `bgDark`, `textInverted`, focus, radius, spacing, and skeleton tokens in both themes.
