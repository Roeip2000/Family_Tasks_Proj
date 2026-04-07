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
