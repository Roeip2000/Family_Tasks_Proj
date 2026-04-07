# Project History

Source of truth for this file: `.ai/raw_claude_cli_full`, especially `file_index.txt`, `all_cli_history_combined.txt`, project session JSONL files, todos, paste-cache files, and project memory.

## Recovered Timeline

### 2026-02-11: Initial deep project audit
- A Claude subagent fully mapped the Android project structure, source files, manifest, Gradle config, resources, Firebase usage, and navigation.
- The recovered early architecture was:
  - Main auth screen with parent register, parent login, and child QR login.
  - Parent dashboard leading to child management, QR generation, task assignment, and template work.
  - Child dashboard fed from Firebase under each child.
- Early Firebase structure already centered on `/parents/{uid}` with nested `children` and `task_templates`.

### 2026-03-18: Child task completion bug fixed
- User reported that a completed task stayed visible and was still marked as late.
- Recovered assistant completion note says the fix did two things:
  - completed overdue tasks no longer display as late
  - completed tasks are counted in summaries but removed from the visible child task list
- This is the earliest clearly confirmed bug fix in the raw history.

### 2026-04-04: Cleanup and repo-state verification
- A later session reported the repo was already on commit `b9e0179` and aligned with `origin/master`.
- That same session refers to code cleanup work being present but not yet committed at that moment.
- This marks the start of the final-polish phase rather than a greenfield rewrite.

### 2026-04-05: Code-style and documentation cleanup wave
- One major session focused on auditing every Java file for:
  - Hebrew comments/Javadoc
  - simpler code style
  - K&R brace style
  - one-line getters/setters
  - removal of dead code, unused imports, and leftover logs
- Recovered concrete code changes from this wave include:
  - `FBsingleton` changed to support callback-based profile saves.
  - `FBsingleton` documented and kept on `updateChildren` to avoid overwriting child/template data.
  - `ParentRegisterFragment` was cleaned and later further improved with validation + async save handling.
- Raw todos from this period show the intent to apply similar cleanup across parent, child, and utility files.

### 2026-04-05: Parent profile image feature investigation and implementation
- A dedicated prompt asked to reuse the existing task-image pipeline for parent profile pictures, without inventing new infrastructure.
- The raw session for the request ended early due context/usage limits, but the current source now shows the feature implemented:
  - parent can pick an image from the gallery
  - image is processed via `ImageHelper`
  - image is saved under the parent profile as `profileImageBase64`
  - parent dashboard shows the image next to the parent name
- This completion is inferred from the current code and consistent with the recovered requirement.

### 2026-04-05 to 2026-04-06: Final product polish phase
- Later prompts shifted from pure cleanup to final-student-project polish:
  - keep folder structure understandable
  - reduce monster files and methods safely
  - keep explanations and comments student-friendly
  - preserve runtime behavior
  - finish parent dashboard clarity and template management
- Recovered target areas from this phase:
  - parent dashboard summaries and filters
  - task template management beyond creation
  - improved child dashboard readability and behavior
  - session handling and logout
  - final review for submission readiness

### 2026-04-06: Simplification and final-review session
- A large session explicitly asked for:
  - student-readable simplification
  - preserving package structure
  - parent dashboard filter correctness
  - derived summaries for assigned/completed/urgent tasks
  - task template edit/delete support
- The raw session ended with a rate-limit message, so not every completion is explicitly narrated.
- Current source strongly suggests many of these changes did land:
  - parent dashboard now has child summaries, task filters, parent profile image, and per-child task views
  - task templates now support create, list, edit, and delete in the same activity
  - child dashboard now has RecyclerView-based task rendering, filters, avatar display, and logout

### 2026-04-07: Another review-oriented continuation started
- A fresh prompt restated the student-level rules and continuation rules.
- That session also ended due usage limits before a final summary was captured.
- No new verified source-code change is recoverable from that session alone.

## Recovered Completed Changes
- Project structure, Firebase paths, and flow architecture were documented early and remained stable.
- Child task completion handling was fixed so completed overdue tasks no longer stay visible as late items.
- Parent profile save logic was made safer through `FBsingleton.updateChildren(...)` instead of subtree replacement.
- `FBsingleton` gained callback-aware save support for safer registration flow completion.
- Parent registration flow gained stronger validation and waits for profile save before opening the dashboard.
- Parent dashboard now includes parent profile image display and image update flow using existing helper infrastructure.
- Parent dashboard now includes child summaries and filter-driven task views in current source.
- Child management now supports list/edit/delete plus child profile image handling in current source.
- Task template management now supports list/edit/delete instead of create-only in current source.
- Child dashboard now uses an adapter-backed task list with filters, avatar loading, and logout in current source.

## Product and UX Requirements Recovered From History
- Keep the project explainable for a 12th-grade student, not enterprise-grade.
- Prefer simple Java and clear method names over advanced abstractions.
- Keep the main entry choices simple: parent register, parent login, child QR login.
- Preserve the package/folder mental map unless there is a strong reason to change it.
- Reuse existing image flow for new image features.
- Keep comments and Javadoc in simple Hebrew when adding or updating inline project documentation.
- Preserve the Firebase path family unless a real regression proves otherwise.
- Parent dashboard should quickly communicate assigned, completed, and urgent work.
- Child dashboard should stay clear and not overloaded.
- QR login should stay simple; current code centers on parent QR plus optional child-specific compatibility.

## What Could Not Be Fully Recovered
- Several April sessions ended with rate-limit/context-limit errors before final completion summaries.
- Some todos exist without a matching explicit "done" message, so completion was inferred from current source where possible.
- `file-history/*` snapshots were not fully mapped back to every exact filename during this recovery pass.
- No final device/emulator verification transcript for the latest April state was found in the raw export.
- No finished in-repo final submission packet or oral-defense cheat sheet was recovered from raw history.
