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
