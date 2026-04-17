# Family Tasks Project Agent Notes

## Startup Checklist
- Before making suggestions, plans, or code edits, read these files first:
  - `AGENTS.md`
  - `CLAUDE.md`
  - `GEMINI.md`
  - `.ai/history/project_history.md`
  - `.ai/decisions/architecture_decisions.md`
  - `.ai/bugs/resolved_issues.md`
  - `.ai/status/current_focus.md`
  - `.ai/status/recovered_sources.md`
- Treat `.ai/raw_claude_cli_full` as the permanent local source of truth for recovered Claude CLI history.
- Do not commit, delete, overwrite, or reorganize the raw archive unless the user explicitly asks.

## Scope
- This repo is a student-level Android final project in Java with Firebase Auth + Firebase Realtime Database.
- This repo is also the user's main study asset for the final oral exam, not only the shipping codebase.
- Use `.ai/raw_claude_cli_full` as the historical source of truth when reconstructing intent or prior work.
- Do not treat `CLAUDE.md`, `GEMINI.md`, `PROJECT_STATE.md`, or `SESSION_HANDOFF.md` as source-of-truth unless they are later validated against raw history.

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

## Final Exam Priority
- Treat this repo as both a codebase and an oral-defense study pack.
- Prefer refactors that make the code easier to explain out loud over refactors that only reduce line count.
- Before changing a large file, classify it:
  - `safe cleanup target`: overloaded but behaviorally stable; extract helpers only.
  - `keep mostly as-is`: linear flow that is easy for the student to explain step-by-step.
- Never aggressively shorten code just to make it look cleaner.
- If a change would make the code harder to present in the exam, prefer the clearer version.

## Exam-Critical Files
- `ParentDashboardActivity`: most complex file; explain filters, summaries, selected child flow, and task actions.
- `ChildDashboardActivity`: explain session resolution, task loading, filters, and mark-done flow.
- `AssignTaskToChildActivity`: explain template autofill, child selection, due date, and Firebase write.
- `ChildSelectionActivity`: explain parent resolution, spinner loading, and child handoff.
- `ChildQRLoginFragment`: explain QR parsing, parent validation, and session save.
- `ManageChildrenActivity`: explain add/edit/delete child flow and image handling.

## Safe Refactor Guidance
- Approved direction for cleanup:
  - extract helper methods from overloaded methods
  - remove duplicated validation code
  - isolate snapshot-to-model mapping logic
  - isolate summary/count calculation logic
  - keep helpers private and local when possible
- High-value examples:
  - `ParentDashboardActivity`: extract snapshot-to-task mapping and household summary calculation helpers
  - `ChildDashboardActivity`: extract star/count summary logic and repetitive filter-UI updates
  - `AssignTaskToChildActivity`: unify repeated validation into one helper
  - `ManageChildrenActivity`: extract form-mode toggles and validation helpers

## Dangerous Refactors To Avoid
- Do not introduce universal/generic Firebase listener abstractions.
- Do not migrate existing flows to fragments or invent new architecture layers late in the project.
- Do not change Firebase paths, QR payload strategy, session strategy, or model field names unless the task explicitly proves a regression.
- Do not replace clear manual Firebase maps with clever abstractions that are harder for the student to explain.

## Memory And Documentation Rule
- When the user asks to "save in memory", prefer durable repo memory:
  - `AGENTS.md`
  - `CLAUDE.md`
  - `GEMINI.md`
  - `.ai/*` history/status files
- Only record concrete, verified information. Do not mark features as complete until the diff is accepted and behavior is verified.

## When Updating Docs
- Update `.ai/history/project_history.md` when new work is completed.
- Update `.ai/decisions/architecture_decisions.md` when a structural rule changes.
- Update `.ai/bugs/resolved_issues.md` for real fixes, not for planned work.
- Update `.ai/status/current_focus.md` for the next actionable work and known gaps.
- Update `.ai/status/recovered_sources.md` when new archive sources are scanned, reclassified, or found to conflict.
