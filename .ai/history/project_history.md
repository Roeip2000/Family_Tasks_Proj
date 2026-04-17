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

## 2026-04-16: Assign-task resource-linking fix
- The build failed in `activity_assign_task_to_child.xml` because both Spinner views referenced `@drawable/bg_spinner`, but that drawable file was missing from `app/src/main/res/drawable/`.
- The fix restored the intended shared style resource by adding `app/src/main/res/drawable/bg_spinner.xml` with the same simple rounded-field look already used by the screen.
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully after the drawable was added.
- Confidence: explicitly verified in the current session

## 2026-04-16: Second-pass UI polish and consistency pass
- A later refinement pass focused on making the already-redesigned UI feel more intentional without changing logic or adding screens.
- Concrete XML-only improvements made in this session:
  - login/register forms gained cleaner spacing plus `imeOptions` and autofill hints for smoother keyboard flow
  - main entry shell and parent dashboard spacing were tightened so primary vs. secondary actions read faster
  - child dashboard task cards were softened and the `Done` button was moved from an off-palette purple to the app accent color
  - QR and child-selection screens were rebuilt into the same card/spacing/color system as the rest of the app
  - manage-children and template screens were brought closer to the newer input/button styling so they no longer looked like older untouched screens
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully after the polish pass.
- Confidence: explicitly verified in the current session

## 2026-04-16: ParentDashboard overload reduction pass
- A focused ParentDashboard redesign was completed to make the screen easier to scan without changing its data model or navigation targets.
- Concrete changes made in this session:
  - the top profile card was simplified into a lighter header row
  - the three summary cards were kept, but the main content was restructured into one larger workspace card instead of separate stacked child/tasks cards
  - the child selector and task list now live in the same card so the screen feels like one flow instead of many competing sections
  - the child selector itself was later refined from a vertical mini-list into a horizontal picker so it stops competing with the task list for vertical attention
  - the visible task tabs were reduced in the UI to `Urgent / Open / Completed`
  - the default active task view was changed from `ALL` to `ASSIGNED` so the screen opens on one focused list instead of multiple grouped sections
  - action buttons were kept at the bottom with one dominant primary button and weaker secondary/destructive actions
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully after the ParentDashboard restructure.
- Confidence: explicitly verified in the current session

## 2026-04-16: Whole-app UI/UX redesign pass for submission quality
- A deeper redesign pass was completed across the actual project screens to reduce the template-like dashboard feel and make the app more believable as a strong student final project.
- This pass preserved the existing functionality and flows:
  - no Firebase path changes
  - no new screens
  - no new product logic
  - mostly XML, drawable, string, and style work with only small Java support edits
- Shared design-language work completed in this session:
  - replaced the old black/white-only palette with a restrained app palette for primary, accent, urgent, soft surfaces, dividers, and quiet destructive states
  - added shared title/section/button styles so screens stop looking like separate iterations
  - rebuilt common field, button, summary-strip, and tab/filter backgrounds to reduce heavy repeated card shapes
- ParentDashboard redesign completed in this session:
  - converted the screen into a lighter header, one quiet summary strip, one main task workspace, and a weaker secondary-action footer
  - refined the child selector into a compact horizontal picker so the task list remains the clear main focus
  - reduced the visible task states to `urgent / open / completed` and styled them as tabs instead of metric chips
  - simplified task rows and child selector rows so the screen stops feeling like stacked generated widgets
- Other screen redesigns completed in this session:
  - `MainActivity` now separates parent actions from child actions more clearly
  - parent login/register were rebuilt as top-aligned forms with better keyboard-safe spacing and direct submit actions from the keyboard
  - child dashboard was softened into a calmer, lighter task screen with less decoration and fewer competing containers
  - child QR login, child selection, QR generation, manage children, assign task, and template management were all brought into the same visual language
- Small Java support changes completed in this session:
  - login/register password fields now submit on `Done`
  - ParentDashboard filter styling was simplified without changing task logic
  - ChildDashboard and task adapters dropped extra motion/gimmick styling so the UI feels more practical
  - `AssignTaskToChildActivity` no longer forces edge-to-edge padding behavior that fought the form layout
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully after the redesign pass.
- Confidence: explicitly verified in the current session

## 2026-04-17: Parent-side refinement pass focused on hierarchy and explainability
- A follow-up redesign pass focused specifically on the remaining parent-side UX issues that were still visible in the current source after the broader 2026-04-16 redesign.
- Concrete changes completed in this session:
  - `ParentDashboard` child selection was reduced from stat-heavy mini cards into a lighter compact selector so the task area reads as the main focus.
  - `ParentDashboard` task tabs now show the filtered tasks directly without repeating a redundant section header for each visible state.
  - `ParentDashboard` task rows were simplified into a cleaner list pattern with one title, one due line, one status chip, and a small status dot instead of hidden child-image scaffolding.
  - `ParentDashboard` and `ManageChildrenActivity` no longer rely on fixed `320dp` list regions; both screens now measure list height to their current content so the screens do not leave large empty blocks on short lists.
  - `MainActivity` was compacted into a smaller entry shell so the login/register fragment gets more room when the keyboard opens.
  - parent login/register spacing was tightened further to keep the submit button closer while typing on smaller phones.
  - `ManageChildrenActivity` gained a clearer form title plus a smaller horizontal photo row to reduce vertical bulk.
  - `ParentTaskTemplateActivity` stopped using the default Android text row and now uses a custom template row with preview image and edit/delete hint.
  - assign-task and QR parent screens gained smaller secondary back buttons and clearer helper labels so they feel like part of the same product.
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully.
  - `./gradlew.bat lintDebug` completed successfully and produced an HTML lint report in `app/build/reports/lint-results-debug.html`.
  - `./gradlew.bat testDebugUnitTest` completed successfully.
- Confidence: explicitly verified in the current session

## 2026-04-17: Whole-app second pass for child-side consistency and auth feedback
- A second follow-up pass extended the redesign beyond the parent flow so the child-side no longer felt like an older leftover screen set next to the newer parent UI.
- Concrete changes completed in this session:
  - `ChildDashboard` was rebuilt from a mini-dashboard stack into a lighter child header plus one focused task workspace card.
  - The child filter controls were simplified from three full visual blocks into a calmer segmented strip that still preserves the same urgent/open/completed behavior.
  - Child task rows were tightened with smaller preview images, quieter completed-state tinting, and a compact stars pill that only appears when relevant.
  - Child QR login and child selection were converted to the same top-aligned scrollable structure as the rest of the app, so the child flow now feels like part of the same product.
  - `ChildSelectionActivity` now changes its subtitle depending on whether the parent was already identified by QR/session or still needs to be chosen.
  - Parent login/register gained clearer loading feedback by disabling fields during submission and swapping the CTA text to a loading state.
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully.
  - `./gradlew.bat lintDebug` completed successfully and rewrote the HTML lint report in `app/build/reports/lint-results-debug.html`.
  - `./gradlew.bat testDebugUnitTest` completed successfully.
- Confidence: explicitly verified in the current session
