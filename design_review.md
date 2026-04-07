# Family Tasks — Parent Dashboard Design Review

## Current UI issues
- The parent dashboard was previously mixing household-level information and child-level task detail too closely.
- The recent refactor had introduced structural clutter and weakened the visual separation between the selected-child area and the rest of the screen.
- The app still relies on a few legacy layout patterns that are functional but not yet polished enough for a larger household with many children.

## Remaining parent-side UX issues
- The quick actions are functional, but the screen still depends on a long vertical flow and can become dense when the task list grows.
- Child cards are now separated correctly, but the selected-child area could still use stronger visual hierarchy so the active child stands out faster.
- Some layouts still depend on placeholder backgrounds rather than stronger empty-state visuals.

## Recommended structure
- Keep the current order:
  - parent header
  - household summary cards
  - child summary list
  - selected-child task section
  - quick actions
- Keep task filtering scoped to the selected child only.
- Keep the child list compact and scrollable; do not return to a design that permanently expands every child’s task set at once.

## Spacing fixes
- Increase the vertical spacing between the child summary list and the selected-child task area.
- Keep card padding consistent across household summary cards, child cards, and task cards.
- Preserve stronger separation between quick actions and the task area so the action row does not visually compete with task content.

## Image placement notes
- Keep the parent profile image in the top header only.
- Keep child images inside child summary cards and task cards where they directly support recognition.
- Preserve placeholder backgrounds when no photo exists so missing images do not collapse the card rhythm.

## Unresolved visual polish items
- Replace API-31-only `clipToOutline` assumptions with a more consistent cross-version avatar treatment if visual polish becomes a higher priority.
- Revisit button styling in the quick-action rows if the dashboard gets a second polish pass.
- Consider improving empty-state visuals and helper text once manual QA confirms the restored flows are stable on device.
