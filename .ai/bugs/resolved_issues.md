# Resolved Issues

Recovered from `.ai/raw_claude_cli_full` and validated where possible against current source.

## RI-001: Completed child tasks stayed visible and could still look late
- First clear evidence: 2026-03-18 session `8e8e82d3-94b2-45a4-8614-5f81ec78ed33`
- Problem:
  - After a child marked a task complete, it stayed in the list and could still be shown as overdue.
- Resolution:
  - Completed tasks are counted for summary/stars but are removed from the visible open-task list.
  - Late/urgency text no longer treats completed tasks as active overdue items.
- Confidence: explicit completion message in raw history

## RI-002: Parent profile save risked overwriting nested parent data
- Evidence:
  - The code-cleanup session explicitly called out the need for `updateChildren()` instead of `setValue()`.
  - Current `FBsingleton` uses `updateChildren(...)`.
- Problem:
  - Replacing the full parent node could wipe existing children or task templates.
- Resolution:
  - Parent profile writes now patch only profile fields.
- Confidence: explicit in raw history and current source

## RI-003: Parent registration flow was missing safe async completion handling
- Evidence:
  - Raw history shows the registration flow being revised and `FBsingleton` gaining a completion callback.
  - Current `ParentRegisterFragment` waits for profile save success before opening the parent dashboard.
- Problem:
  - Registration could move forward before the parent profile write fully completed.
- Resolution:
  - Registration now validates inputs, shows loading state, saves the profile, and only then opens the dashboard.
- Confidence: explicit in raw history and current source

## RI-004: Parent registration/login error handling around Firebase exceptions was hardened
- Evidence:
  - Raw history repeatedly mentions null-safe `task.getException()` handling and better error reporting.
  - Current `ParentRegisterFragment` null-checks registration errors.
- Problem:
  - Error handling could produce weak feedback and risk null-related issues.
- Resolution:
  - Null-safe exception-message extraction was added.
  - User-facing validation improved for email/password input.
- Confidence: explicit in raw history and current source

## RI-005: Child-management was no longer add-only in the final code state
- Evidence:
  - Raw history contains a strong user complaint that child management only supported adding children.
  - Current `ManageChildrenActivity` supports load/list/edit/delete and photo updates.
- Problem:
  - Parent could not review or manage existing children from the management screen.
- Resolution:
  - The screen now loads current children and provides edit/delete behavior in-place.
- Confidence: inferred from current source; explicit success summary not found in raw logs

## RI-006: Task-template management was expanded beyond create-only
- Evidence:
  - A late April prompt explicitly required edit/delete support.
  - Current `ParentTaskTemplateActivity` supports list/create/edit/delete.
- Problem:
  - Templates were previously creation-only, which was not enough for final-project polish.
- Resolution:
  - Existing template management screen now handles edit/delete with confirmation and form reset.
- Confidence: inferred from current source; raw session ended before final completion summary

## RI-007: Assign-task screen failed Android resource linking because `bg_spinner` was missing
- First clear evidence: 2026-04-16 current-session build error from `activity_assign_task_to_child.xml`
- Problem:
  - The assign-task layout referenced `@drawable/bg_spinner` for two Spinner fields, but the drawable file did not exist in `app/src/main/res/drawable/`.
  - This caused `Android resource linking failed` and stopped the build.
- Resolution:
  - Added `app/src/main/res/drawable/bg_spinner.xml` as the missing rounded field background.
  - Verified the fix with `./gradlew.bat assembleDebug`.
- Confidence: explicit current-session verification

## RI-008: Several polished screens still had leftover pre-redesign styling and weak input flow details
- First clear evidence: 2026-04-16 XML review of the current source after the first redesign wave
- Problem:
  - Some secondary screens still used hardcoded old colors, default spinner/input styling, and inconsistent button sizing.
  - Login/register forms also lacked keyboard-flow hints such as next/done actions and autofill metadata.
  - The child task card still used an off-palette purple CTA that conflicted with the rest of the app.
- Resolution:
  - Updated auth, QR, child-selection, template, manage-children, and dashboard item layouts to the same spacing/color/card language.
  - Added `imeOptions` and autofill hints to parent login/register fields.
  - Switched the child task `Done` button to the app accent color and softened card elevations/borders.
  - Verified the full UI polish pass with `./gradlew.bat assembleDebug`.
- Confidence: explicit current-session verification

## RI-009: ParentDashboard still felt overloaded because children and tasks were split into competing full-weight sections
- First clear evidence: 2026-04-16 focused ParentDashboard review
- Problem:
  - The screen stacked too many equally heavy sections, which made it harder to understand quickly.
  - It also opened in an `ALL` task mode that showed multiple grouped lists at once.
- Resolution:
  - Rebuilt the screen into a lighter header, summary cards, one main workspace card, and a quieter action footer.
  - Merged child selection and task viewing into the same main card.
  - Changed the child selector from a vertical stacked list into a horizontal picker to keep the tasks area visually primary.
  - Changed the default task filter to `ASSIGNED` and hid the `ALL` tab from the visible layout.
  - Verified the redesign with `./gradlew.bat assembleDebug`.
- Confidence: explicit current-session verification

## RI-010: Final app screens still looked inconsistent, too card-heavy, and too template-like
- First clear evidence: 2026-04-16 full UI audit of the current repository state
- Problem:
  - Several screens still felt like separate iterations of the project, with uneven spacing, repeated rounded boxes, weak action hierarchy, and a generic generated-dashboard feel.
  - Parent login/register also remained uncomfortable on smaller screens when the keyboard opened.
- Resolution:
  - Introduced a shared visual system through colors, styles, button backgrounds, quieter surfaces, and more consistent typography.
  - Rebuilt `MainActivity`, auth screens, ParentDashboard, ChildDashboard, QR/login helper screens, manage-children, assign-task, and template management layouts into one calmer RTL Hebrew-first language.
  - Top-aligned the auth forms, increased keyboard-safe bottom padding, kept scrolling natural, and added keyboard `Done` submission on password fields.
  - Verified the redesign pass with `./gradlew.bat assembleDebug`.
- Confidence: explicit current-session verification

## RI-011: ParentDashboard still gave too much weight to child cards and left empty task space on short lists
- First clear evidence: 2026-04-17 parent-side UX review of the current repository state
- Problem:
  - The selected-child area still carried too much visual detail, so it competed with the actual task list.
  - The task area still used a fixed-height `ListView`, which left large empty blocks when only a few tasks existed.
- Resolution:
  - Rebuilt the child selector into a lighter compact format with only the child name and current open/urgent state.
  - Simplified task rows and removed redundant single-tab section headers.
  - Added content-height measurement for the dashboard task list so the screen grows only as much as the current task data requires.
  - Verified with `./gradlew.bat assembleDebug` and `./gradlew.bat lintDebug`.
- Confidence: explicit current-session verification

## RI-012: Parent management screens still had unfinished default rows and bulky vertical form structure
- First clear evidence: 2026-04-17 review of `ManageChildrenActivity`, `ParentTaskTemplateActivity`, `MainActivity`, `AssignTaskToChildActivity`, and `GenerateQRActivity`
- Problem:
  - Manage-children still used a large vertical photo block plus a fixed-height child list.
  - Task templates still used the plain Android `simple_list_item_1`, which made the screen feel older than the rest of the app.
  - The parent entry shell still gave too much room to navigation chrome instead of the active login/register form.
- Resolution:
  - Compressed the parent entry action panel for better keyboard space.
  - Rebuilt the manage-children form into a clearer title + compact photo row and made its child list content-sized.
  - Replaced the template default list row with a custom row that includes preview image and edit/delete guidance.
  - Added small helper labels to assign-task and QR screens so the flows look intentional without adding new logic.
  - Verified with `./gradlew.bat assembleDebug` and `./gradlew.bat lintDebug`.
- Confidence: explicit current-session verification

## RI-014: Child QR, quick-child button, and parent "all children" flow were still fragmented
- First clear evidence: 2026-04-18 integrated product pass review of `ChildQRLoginFragment`, `MainActivity`, and `ParentDashboardActivity`
- Problem:
  - Scanning a child-specific QR (`parent:X|child:Y`) still routed the child through `ChildSelectionActivity`, forcing a redundant name pick even though the QR already identified the child.
  - The non-QR quick-child button always opened a parent spinner — even when a full child session was already saved — and it also did not explain the first-time experience.
  - ParentDashboard had no real "all children" mode: the parent could see one child at a time but not a household-wide view, so they could not answer "what's open in the house right now?" from a single screen.
  - Tasks in a hypothetical all-children view had no child-name affordance, and per-child chips did not show compact counts.
- Resolution:
  - `ChildQRLoginFragment` now jumps straight to `ChildDashboardActivity` when the QR carries both ids; parent-only QR still opens `ChildSelectionActivity`.
  - `MainActivity.openChildQuickLogin()` prefers a saved full session, falls back to saved parent-only, and otherwise shows a short first-time dialog (scan QR positive / manual negative / cancel neutral) instead of the old spinner shortcut.
  - `ParentDashboardActivity` injects a synthetic `ALL_CHILDREN_ID` chip at the head of the child row, defaults the selection to it, aggregates tasks across all children, and toggles `tvTaskOwner` on each row so ownership is explicit; empty-state text switches to household strings.
  - Per-child chips now show `N פתוחות · M דחופות` via `item_parent_child_summary.xml` and strings already present in the project.
  - Completed-task read-only behavior and end-to-end star flow were re-verified in the current source; no code change was needed for those two.
  - Verified with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug`.
- Confidence: explicit current-session verification

## RI-013: Child-side screens still looked like an older mini-dashboard and did not match the refined parent flow
- First clear evidence: 2026-04-17 whole-app redesign review of `ChildDashboard`, `ChildQRLoginFragment`, and `ChildSelectionActivity`
- Problem:
  - The child dashboard still used three separate metric-like filter blocks plus a separate task area, which recreated the same stacked-widget problem that had already been fixed on the parent side.
  - Child QR login and child selection were functional, but their structure still felt disconnected from the rest of the redesigned app.
  - Parent login/register still lacked stronger inline submission feedback while waiting on Firebase.
- Resolution:
  - Rebuilt `ChildDashboard` into a lighter top header and one task workspace card with calmer segmented filters.
  - Simplified child task rows and hid the stars pill when it is not relevant.
  - Converted child QR login and child selection into the same top-aligned scroll-based structure used elsewhere in the app.
  - Added subtitle logic in `ChildSelectionActivity` so the child understands whether the parent was already identified.
  - Added loading-state CTA text and field disabling in parent login/register.
  - Verified with `./gradlew.bat assembleDebug`, `./gradlew.bat lintDebug`, and `./gradlew.bat testDebugUnitTest`.
- Confidence: explicit current-session verification

## RI-015: The app still looked like separate school screens instead of one polished final-project product
- First clear evidence: 2026-04-18 design audit of the current working tree before the presentation-level UI pass
- Problem:
  - Home still felt like a technical form with one shared action area instead of two clear user roles.
  - Parent dashboard was functional but not impressive enough as a presentation screen.
  - Child task cards looked repetitive, template management still felt cramped, and some secondary/back buttons looked too dark or inactive.
  - Old generic placeholders made the project look unfinished.
- Resolution:
  - Rebuilt the home screen around a warm hero and two explicit role cards for parent and child.
  - Reworked ParentDashboard into a clearer control-center hierarchy and simplified the child/task/action sections.
  - Refined ChildDashboard, template repository, assign-task, manage-children, child-selection, and QR screens into one shared visual language.
  - Replaced generic placeholder visuals with project-owned vector placeholders and small family/task icons.
  - Expanded shared colors, styles, and drawable resources so contrast and button hierarchy stay consistent across screens.
  - Verified with `./gradlew.bat assembleDebug`.
- Confidence: explicit current-session verification
