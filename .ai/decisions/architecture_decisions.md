# Architecture Decisions

Recovered from `.ai/raw_claude_cli_full` and cross-checked against the current source tree.

## AD-001: Keep Firebase centered under `/parents/{uid}`
- Status: active
- Decision:
  - Parent profile data lives under `/parents/{uid}`.
  - Children live under `/parents/{uid}/children/{childId}`.
  - Child tasks live under `/parents/{uid}/children/{childId}/tasks/{taskId}`.
  - Task templates live under `/parents/{uid}/task_templates/{templateId}`.
- Why:
  - This path family appears in early project-memory reconstruction and remains consistent with the current app code.
  - Multiple later prompts explicitly asked to preserve the existing Firebase path family.

## AD-002: Parent profile updates must use `updateChildren`, not `setValue`
- Status: active
- Decision:
  - `FBsingleton.saveParentToFirebase(...)` should patch profile fields only.
- Why:
  - Raw history explicitly calls out subtree-overwrite risk.
  - This prevents losing nested `children` and `task_templates` under a parent.

## AD-003: Reuse one image pipeline everywhere possible
- Status: active
- Decision:
  - Use `ImageHelper` for gallery pick handling, EXIF correction, Base64 conversion, and circular avatar display.
  - Reuse that path for task images, child profile images, and parent profile images.
- Why:
  - The user explicitly asked not to invent a new image system if the project already had one.
  - Current source reflects this reuse pattern.

## AD-004: Preserve the package mental map
- Status: active
- Decision:
  - Keep auth, parent, child, Firebase, and util code in their current logical areas.
  - Avoid broad refactors or package explosion.
- Why:
  - Multiple April prompts repeat this as a hard rule for student readability.

## AD-005: Keep the QR flow simple and backward-compatible
- Status: active
- Decision:
  - Current primary QR payload is `parent:{parentId}`.
  - `ChildQRLoginFragment` still accepts `parent:{parentId}|child:{childId}` and raw child-id style inputs for compatibility.
- Why:
  - History shows conflicting experiments and plans around QR payload changes.
  - Current source resolves this by keeping the flow simple while preserving compatibility.

## AD-006: Use local session persistence for child flow
- Status: active
- Decision:
  - Persist `parentId` and `childId` in `SharedPreferences` under `child_session`.
- Why:
  - Recovered memory and current source both rely on this for child flow continuity and fallback when intent extras are missing.

## AD-007: Derive parent dashboard summaries from existing task data
- Status: active
- Decision:
  - Compute assigned/completed/urgent counts from task state instead of adding new backend counters.
- Why:
  - A later April prompt explicitly requested derived summaries rather than inventing a new backend model.
  - Current dashboard code follows that approach.

## AD-008: Expand management inside existing screens before adding new ones
- Status: active
- Decision:
  - Add edit/delete management inside current child-management and task-template screens instead of introducing new activities or fragments.
- Why:
  - The user repeatedly asked for minimal, safe changes and rejected new screens/classes unless strictly necessary.
  - Current source implements this pattern.

## AD-009: Restore missing shared UI resources instead of rewriting working layout references
- Status: active
- Decision:
  - If a screen already references a shared drawable/style resource and the build breaks only because that resource file is missing, restore the resource first.
- Why:
  - This keeps fixes local, preserves the intended screen styling, and avoids unnecessary layout churn in a student-level project.
  - The 2026-04-16 `bg_spinner` fix for the assign-task screen followed this rule and cleared the resource-linking failure without changing screen logic.

## AD-010: Late UI polish should stay XML-only and inside the current visual system
- Status: active
- Decision:
  - Final submission polish should prefer XML spacing, color, input, and card refinements over logic changes or new UI flows.
  - Reuse the existing palette and shared field/card resources instead of introducing a second styling system.
- Why:
  - The project is already functionally complete; the highest-value remaining polish is clarity and consistency.
  - The 2026-04-16 second-pass refinement was completed entirely through small XML edits and preserved the student-readable structure.

## AD-011: ParentDashboard should open in a focused task mode, not an everything-at-once mode
- Status: active
- Decision:
  - The ParentDashboard task area should default to the `assigned/open` filter instead of the `all` view.
  - The `all` filter can remain in code for compatibility, but it should not dominate the visible UI.
- Why:
  - The main dashboard problem was cognitive overload from showing too much at once.
  - Defaulting to one focused list fits the screen's purpose better and matches the 2026-04-16 overload-reduction pass.

## AD-012: Final UI redesign should stay task-first, lighter, and mostly resource-driven
- Status: active
- Decision:
  - Prefer XML/layout/style/drawable/string changes over logic rewrites for final-stage UI redesign.
  - Reduce repeated full-weight cards and keep one clear focal area per screen.
  - ParentDashboard should stay task-first: compact header, quiet summary, focused child selector, one visible task state at a time, and one dominant primary action.
- Why:
  - The project's main remaining weakness was presentation quality and scanability, not missing business logic.
  - This keeps the redesign explainable in an oral exam and avoids risky late-stage architecture churn.

## AD-013: On parent scroll screens, selectors should stay compact and embedded lists should size to content
- Status: active
- Decision:
  - Keep the selected-child control lightweight and summary-light so the task list remains the dominant content.
  - Avoid fixed-height `ListView` blocks inside parent `ScrollView` screens when the content is short; size them to their current items instead.
- Why:
  - The remaining parent-side UX problems were caused more by layout mechanics and competing section weight than by missing features.
  - Compact selectors plus content-sized lists keep the UI calmer, more believable, and easier to explain in a school presentation.

## AD-014: Child-side screens should stay simpler than the parent side, but still follow the same visual grammar
- Status: active
- Decision:
  - Keep the child experience lighter than the parent experience: one compact header, one focused task workspace, and minimal supporting chrome.
  - Reuse the same button, spacing, card, and muted-surface language across child QR login and child selection instead of inventing a second styling approach.
  - Use inline loading feedback on auth buttons instead of extra flows or dialogs.
- Why:
  - The whole-app review showed that the child flow was still drifting back toward the older "separate classroom screens" feel.
  - A shared visual grammar with a simpler child hierarchy improves cohesion without making the child side too dense or hard to explain.
