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

## 2026-04-18: Integrated product pass — unifying parent and child flows
- A cohesion pass turned the previously separate parent/child flows into one clearly connected app.
- Concrete changes completed in this session:
  - `ChildQRLoginFragment` now recognizes the full `parent:X|child:Y` payload and skips `ChildSelectionActivity`, going straight to `ChildDashboardActivity`. Parent-only QR keeps the old selection step as a fallback.
  - `MainActivity` quick-child button was rewired: saved full session goes directly to the child dashboard; saved parent-only goes to child selection; no saved data opens a short first-time dialog (scan QR / manual / cancel) instead of showing the full parent spinner.
  - Four new dialog strings added in `strings.xml` for the first-time child experience (`child_first_time_title/message/scan/manual`).
  - `ParentDashboardChildSummaryAdapter` gained `ALL_CHILDREN_ID` constant and skips avatar binding for the synthetic "כל הילדים" chip.
  - `item_parent_child_summary.xml` reactivated the compact task count line so each child chip shows `N פתוחות · M דחופות`.
  - `item_parent_task.xml` added a small `tvTaskOwner` line (default `gone`) so task rows can reveal which child a task belongs to.
  - `ParentDashboardTaskAdapter` got a `setShowChildName(boolean)` toggle that reveals the owner line with `parent_dashboard_task_owner_label` (`ל-%1$s`) when active.
  - `ParentDashboardActivity` computes household totals, injects the synthetic "כל הילדים" chip at the head of the child row on every refresh, defaults the selection to it, iterates all tasks when it is active, and routes empty-state text through dedicated household strings. The selected-child header switches to the shared "משימות של כל הילדים" title in this mode.
  - Verified in source (no code rewrite required): completed tasks are already read-only in `showTaskOptionsDialog` (only the close button is shown), and stars flow end-to-end via `TaskTemplate.safeStarsWorth()` → `AssignTaskToChildActivity` → `ChildTask.starsWorth` → child dashboard totals.
- Verification in this session:
  - `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug` completed successfully.
- Confidence: explicitly verified in the current session.

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

## 2026-04-18: Presentation-level UI pass guided by Figma MCP
- A stronger visual pass pushed the app closer to a polished final-project presentation state without changing Firebase, QR, session, stars, package structure, or architecture.
- Figma usage in this session:
  - ran the Figma MCP `create_design_system_rules` flow with `clientLanguages = "Java, XML"` and `clientFrameworks = "Android"`
  - used that guidance to tighten project rules around tokens, card hierarchy, role separation, and resource-driven polish
- Concrete design work completed in this session:
  - `activity_main.xml` was redesigned around a warm hero plus two clear role cards (`אני הורה` / `אני ילד`) instead of one mixed technical action area
  - `activity_parent_dashboard.xml` was rebuilt into a clearer control-center hierarchy with hero summary, child-selection card, focused task workspace, and separate quick actions
  - `activity_child_dashboard.xml` was simplified into a friendlier hero plus calmer task workspace, with task-card surfaces varying by state instead of repeating the same visual block
  - `activity_parent_task_template.xml` became a guided repository screen with a cleaner create-template form and better template scanability
  - `activity_assign_task_to_child.xml`, `activity_manage_children.xml`, `activity_child_selection.xml`, and `activity_generate_qr.xml` were brought into the same product language
  - old generic placeholders were replaced with project-owned placeholder vectors such as `ic_avatar_placeholder`, `ic_image_placeholder`, `ic_family_cluster`, and a simple due-date icon
  - shared `colors.xml`, `styles.xml`, and drawable resources were expanded so secondary/back buttons stay readable and the app no longer feels like disconnected XML drafts
- Minimal Java-only support work completed in this session:
  - avatar fallback handling was updated in parent/child management and dashboard code
  - parent child-summary rows and child task rows received UI-only state and placeholder improvements
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully
  - `adb devices -l` returned no connected device or emulator, so no live emulator verification was possible in this session
- Confidence: explicitly verified in the current session

## 2026-04-22: Code explainability and presentation-readiness pass
- Completed a non-feature, non-design pass focused on making the current code easier to explain in an oral exam.
- Concrete work completed:
  - added short Hebrew comments near QR parsing, child session handling, Firebase path usage, task loading, dynamic stars, all-children filtering, and completed-task read-only context
  - added XML comments before major layout sections such as hero/header, role cards, selectors, task lists, forms, QR card, and quick actions
  - reformatted `ManageChildrenActivity` and `AssignTaskToChildActivity` for readability without changing Firebase paths or runtime behavior
  - created `PROJECT_PRESENTATION_GUIDE.md` with Hebrew project summary, flows, Firebase/QR/session explanations, rubric mapping, oral-exam questions, and a code navigation map
  - created `CODE_EXPLAINABILITY_REPORT.md` summarizing inspected files, changed files, intentional non-changes, and build status
  - removed the visible internal wording "(לבוחן להצגה)" from the quick child-select label while keeping the same string resource id
- Verification in this session:
  - `./gradlew.bat assembleDebug` completed successfully
  - no Firebase paths, QR format strings, package declarations, or layout ids were intentionally changed
- Confidence: explicitly verified in the current session

## 2026-04-22: Family Tasks class diagram created
- Created a UX/FigJam class-style diagram for the current Android Java source tree.
- Verified the included top-level app classes from `app/src/main/java` and included the requested important inner classes:
  - `ParsedQr`, `ChildSelectionActivity.ParentItem`, `ChildSelectionActivity.ChildItem`
  - `ChildTaskAdapter.TaskViewHolder`, `ParentTaskTemplateActivity.TemplateListAdapter`
  - `ManageChildrenActivity.ChildItem`
- The diagram groups classes as screens/fragments, models, adapters, utils/Firebase, and marked external Android/Firebase/library APIs separately.
- No Android app source code, Firebase paths, dependencies, or generated Gradle files were changed.
- Confidence: explicitly verified from the current source tree in this session

## 2026-04-22: Code-generated PlantUML class diagrams created
- Replaced the previous visual-only class-diagram approach with code-generated UML artifacts for the project book.
- Added a documentation-only generator at `docs/uml/tools/GenerateFamilyTasksUml.java`.
- The generator scans `app/src/main/java` with JavaParser from the local Gradle distribution cache and writes PlantUML files under `docs/uml/plantuml/`.
- Rendered the diagrams with the standalone PlantUML jar under `docs/uml/tools/plantuml.jar`.
- Generated separate readable diagrams for:
  - full overview
  - screen classes
  - model classes
  - adapter classes
  - utils/Firebase classes
- Exported SVG and high-resolution PNG images under `docs/uml/images/`.
- Created a PDF readability proof at `docs/uml/pdf/family_tasks_class_diagrams_pdf_check.pdf`.
- `ParentInFb` was the only unconnected project class in the full generated overview and was placed in the diagram's unconnected area.
- No Android app source code, Firebase paths, package declarations, or app behavior were changed.
- Confidence: explicitly generated and verified from current source in this session

## 2026-04-23: User-authored UML draft files created
- Created documentation-only UML draft artifacts requested by the user:
  - `docs/uml/class_inventory.md`
  - `docs/uml/class_relations.md`
  - `docs/uml/project-uml-draft.puml`
- The draft uses only user-authored classes, interfaces, and enums from `app/src/main/java`.
- Generated, framework, Android SDK, Jetpack, Firebase, ZXing, and other library classes are excluded as UML nodes.
- The PlantUML draft groups classes by the current package/layer structure and keeps `ParentInFb` as an unconnected project model because no direct project-owned relation was found in current source.
- Verification in this session:
  - generated-name scan found no excluded generated class names in the new UML draft files
  - `java -jar docs/uml/tools/plantuml.jar -checkonly docs/uml/project-uml-draft.puml` completed successfully
- No Android app source code, Firebase paths, package declarations, generated files, or rendered UML images were changed.
- Confidence: explicitly verified from the current source tree in this session

## 2026-04-26: Simple-Java listener cleanup and session-return fixes
- Completed a Java-only simplification pass across the active app source files.
- Concrete bug fixes completed:
  - `MainActivity` now checks `FirebaseAuth.getCurrentUser()` before `setContentView()` and opens `ParentDashboardActivity` directly for a signed-in parent.
  - `MainActivity` now checks `SharedPreferences("child_session")` before showing the login UI and opens `ChildDashboardActivity` directly when both `parentId` and `childId` are saved.
  - The first-time child dialog was removed from `openChildQuickLogin()`; no saved child session now opens `ChildQRLoginFragment` directly.
  - `ChildSelectionActivity.resolveIds()` now documents that Intent extras are preferred and `SharedPreferences` is only a fallback.
- Simple-Java cleanup completed:
  - replaced lambdas and method references with anonymous listener classes
  - moved ActivityResult callbacks into named handler methods
  - broke Firebase task writes/listeners into separate reference/task variables where they were chained
  - replaced non-trivial ternaries with if/else helpers in the edited code
  - added Hebrew comments and named helper methods around Firebase, SharedPreferences, Intent extras, validation, adapter binding, and image handling
- Verification in this session:
  - source scans found no remaining `->` lambdas or `::` method references under `app/src/main/java/com/example/family_tasks_proj`
  - source scans found no direct AlertDialog lambda patterns or chained Firebase task listener patterns checked by the session scan
  - `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" .\gradlew.bat assembleDebug` completed successfully
- Confidence: explicitly verified in the current session
