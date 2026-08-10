# BcSheetShell

**Status:** Implemented
**Surface:** Shared sheets and overlays  
**Layer:** Core design-system foundation

## Purpose

Standardize bottom-sheet width, header, scrolling body, footer actions, dismissal, focus, and loading/error structure across feature sheets.

## Contract

```kotlin
@Composable
fun BcSheetShell(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    closeContentDescription: String = "Close",
    body: @Composable ColumnScope.() -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
)
```

Dirty guards, domain state, validation, and confirmation visibility remain feature-owned. Shell coordinates layout and accessible close behavior.

## Anatomy

```text
Sheet surface
    drag handle (optional)
    BcSheetHeader(title, close)
    scrollable body
    optional sticky footer
```

- `bgPrimary` surface, existing sheet radius/elevation tokens.
- Header remains visible while body scrolls.
- Footer actions remain visible/reachable when present.
- Full available width on mobile; bounded width on wider screens.

## Behavior

- Dismiss callback is explicit and may be guarded by feature dirty state.
- Close control remains available when drag dismissal is disabled.
- On dismissal, focus returns to invoking control.
- Child picker/dialog overlays suspend parent interaction and restore context on close.

## States

- Loading: shell/header remain; body skeletons; progress semantics.
- Error: body-level inline error; retry remains inside affected operation.
- Submit busy: footer actions disabled and operation label changes.
- Dirty close: feature presents `Keep editing` / destructive discard confirmation.

## Accessibility

- Title exposed as modal heading.
- Close action has explicit content description.
- Modal semantics trap focus while open.
- Body scroll semantics remain available.
- Footer controls meet `spacing12` (48dp).

## Responsive

- Mobile-first full width.
- Body scrolls independently from sticky footer.
- Two-column form rows collapse when space is tight.
- No fixed widths.

## Theme

Use semantic surface, text, border, focus, disabled, error, radius, spacing, and shadow tokens in both themes.
