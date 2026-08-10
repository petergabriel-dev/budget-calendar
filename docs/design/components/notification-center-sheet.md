# Notification Center Sheet

**Status:** Implemented
**Surface:** Global notification overlay  
**Layer:** Shared overlay

## Purpose

Review, expand, and dismiss generated budget notifications without leaving current page.

## Anatomy

```text
Notifications                              Clear all
notification row
notification row
```

- Empty state: `No new notifications`.
- Rows show title, relative timestamp, type/count badge when relevant, expand control, and dismiss action.
- Expanded content may show overdue items, rollover context, or Safe to Spend warning.
- `Clear all` appears only when notifications exist.

## Behavior

- Header bell opens sheet from any tab.
- Row expansion is independent; expanded state remains while list is open.
- Dismiss removes one notification.
- Clear all confirms only if product policy requires; operation feedback remains visible.
- Overdue details remain read-only links/context; transaction details owns editing.

## Accessibility

- Sheet heading is `Notifications`.
- Expand action exposes expanded/collapsed state and notification title.
- Dismiss action identifies notification.
- Badges/counts are announced as content, not color-only.
- Empty/error states are announced.
- Focus returns to bell on close.
- All controls meet `spacing12`.

## Responsive and theme

- Full-width scrollable list; bounded sheet width on wider layouts.
- Rows keep title and dismiss action visible; expanded content wraps.
- Use `bgSurface`, semantic status, border, focus, radius, spacing, and shadow tokens.
