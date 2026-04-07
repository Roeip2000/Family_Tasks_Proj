# Family Tasks Project Agent Notes

## Startup Checklist
- Before making suggestions, plans, or code edits, read these files first:
  - `AGENTS.md`
  - `CLAUDE.md`
  - `.ai/history/project_history.md`
  - `.ai/decisions/architecture_decisions.md`
  - `.ai/bugs/resolved_issues.md`
  - `.ai/status/current_focus.md`
  - `.ai/status/recovered_sources.md`
- Treat `.ai/raw_claude_cli_full` as the permanent local source of truth for recovered Claude CLI history.
- Do not commit, delete, overwrite, or reorganize the raw archive unless the user explicitly asks.

## Scope
- This repo is a student-level Android final project in Java with Firebase Auth + Firebase Realtime Database.
- Use `.ai/raw_claude_cli_full` as the historical source of truth when reconstructing intent or prior work.
- Do not treat `CLAUDE.md`, `PROJECT_STATE.md`, or `SESSION_HANDOFF.md` as source-of-truth unless they are later validated against raw history.

## Product Summary
- Main app purpose: parent-managed family task tracking.
- Parent flow: register/login, manage children, generate QR, assign tasks, manage task templates, review household progress in the parent dashboard.
- Child flow: scan QR, select child when needed, open child dashboard, mark tasks complete, view stars and urgency.

## Current Package Map
- `auth`: entry activity and main auth shell.
- `Parents`: parent login/register fragments.
- `Parents_Dashbord_and_mange`: parent dashboard, child management, QR generation, task assignment, template management, parent-side adapters/models.
- `Child_Login`: QR login and child selection flow.
- `child`: child dashboard and child task adapter.
- `child.model`: child-side models.
- `FireBase`: Firebase singleton + parent model.
- `util`: shared helpers such as `ImageHelper`, `DateUtils`, `NameUtils`.

## Recovered Architecture Rules
- Preserve the current package mental map. Prior history repeatedly rejected broad reorganizations.
- Keep code student-readable. Prefer short helpers, simple Java, and small safe edits over "enterprise" structure.
- Keep comments and Javadoc simple and easy for a 12th-grade student to explain orally.
- Reuse existing image infrastructure instead of adding new libraries or image systems.
- Preserve the current Firebase path family unless a concrete regression proves it is wrong.
- Preserve the simple QR flow. Current source uses a parent QR payload by default and still accepts legacy parent/child payloads.

## Recovered Data Model
- Parent profile root: `/parents/{uid}`
- Child records: `/parents/{uid}/children/{childId}`
- Child tasks: `/parents/{uid}/children/{childId}/tasks/{taskId}`
- Task templates: `/parents/{uid}/task_templates/{templateId}`
- Parent profile image field in current source: `profileImageBase64`
- Child profile image field in current source: `profileImageBase64`

## Important Implementation Conventions
- `FBsingleton` writes parent profile fields with `updateChildren`, not `setValue`, to avoid overwriting child/template subtrees.
- `ImageHelper` is the shared path for image pick/load, EXIF correction, Base64 conversion, and circular avatar rendering.
- Parent dashboard summaries should be derived from task state, not from new backend counters.
- Child session persistence uses `SharedPreferences` under `child_session`.

## Recovered Open Work
- Verify the latest parent/child flows end-to-end on device or emulator; the latest raw sessions hit rate limits before a full final verification record.
- Review `AssignTaskToChildActivity` hardcoded `starsWorth = 10` and decide whether that is acceptable for submission.
- Decide whether parent login still needs a loading indicator and any final UX polish before submission.
- Confirm whether any remaining final-report or demo-prep docs are still needed; raw history shows repeated review requests but no finished submission packet inside the repo.

## When Updating Docs
- Update `.ai/history/project_history.md` when new work is completed.
- Update `.ai/decisions/architecture_decisions.md` when a structural rule changes.
- Update `.ai/bugs/resolved_issues.md` for real fixes, not for planned work.
- Update `.ai/status/current_focus.md` for the next actionable work and known gaps.
- Update `.ai/status/recovered_sources.md` when new archive sources are scanned, reclassified, or found to conflict.
