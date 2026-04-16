# Claude Project Bootstrap

## Read First
- Before making suggestions, writing plans, or editing code, read:
  - `AGENTS.md`
  - `CLAUDE.md`
  - `.ai/history/project_history.md`
  - `.ai/decisions/architecture_decisions.md`
  - `.ai/bugs/resolved_issues.md`
  - `.ai/status/current_focus.md`
  - `.ai/status/recovered_sources.md`

## Historical Source Of Truth
- Use `.ai/raw_claude_cli_full` as the full saved local Claude CLI history for this project.
- Prefer explicit evidence from raw session files over stale summary docs.
- If raw history conflicts with current source or with other recovered notes, record the conflict explicitly instead of guessing.

## Repository Rules
- Do not modify app source code until the task explicitly calls for it.
- Keep raw Claude archives and snapshots local.
- Keep durable project memory files tracked in Git.
- Do not delete or overwrite `.ai/raw_claude_cli_full` or `.ai/snapshots` unless the user explicitly requests it.

## Project Constraints
- This is a student-level Android Java project and should stay explainable and simple.
- Preserve the current package mental map unless there is a concrete reason to change it.
- Prefer small, safe edits over broad refactors.

## End Of Session Rule
Before ending any meaningful session, update:
- .ai/history/project_history.md
- .ai/decisions/architecture_decisions.md
- .ai/bugs/resolved_issues.md
- .ai/status/current_focus.md
- .ai/status/recovered_sources.md

Rules for updates:
- add only concrete new information from the current session
- do not rewrite old content unnecessarily
- clearly mark anything inferred versus explicitly verified
- if a bug was fixed, record the confirmed fix
- if a decision was made, record the decision and why
- if work is still incomplete, record it under pending work
