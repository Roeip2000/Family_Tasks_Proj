# Recovered Sources

Purpose: record which raw Claude CLI archive files were scanned, which were treated as project-relevant, which were classified as noise, and where uncertainty still exists.

## Primary Source Of Truth
- `.ai/raw_claude_cli_full/`
- Most important top-level files:
  - `.ai/raw_claude_cli_full/file_index.txt`
  - `.ai/raw_claude_cli_full/all_cli_history_combined.txt`
  - `.ai/raw_claude_cli_full/history.jsonl`

## Project-Relevant Sources Scanned
- Project memory:
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/memory/MEMORY.md`
- Main project session JSONL files:
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/0e295343-91e7-4fcc-a6fa-f9409ac3f392.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/4483ed3b-6d54-4bc7-bb65-0587df387871.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/541068c4-308b-40cd-b9d1-cdb814110da0.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/580538a3-a255-45cd-b94d-2659f5eeadd4.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/61c79d9a-f3f6-4b64-8357-de271926e236.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/81355c0a-7e24-4e56-8a0f-858057b88744.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/8184253d-46f1-42b8-8830-fca860e6a5f5.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/8e8e82d3-94b2-45a4-8614-5f81ec78ed33.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/99c84aa2-6aa4-4e69-a574-fc74f6bc0cd0.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/c7593e05-68e6-4cf3-b854-51403dbc7685.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/d64d3c07-af87-4fcf-bab3-5a4488bc23bd.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/d831ed33-5cde-4a81-aeb4-d0fb1373d479.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/fa3a102e-31e9-43cc-9ca1-241f470d576e.jsonl`
- Project worktree session traces:
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj--claude-worktrees-stupefied-nobel/3997c3c3-a881-4c29-9388-4b8a84804cd4.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj--claude-worktrees-thirsty-banach/4c49fc5b-4e04-4893-94bc-c18bb5a98d98.jsonl`
- Project-related subagent traces:
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic-AndroidStudioProjects-Family-Tasks-Proj/**/subagents/*.jsonl`
- Project todo snapshots:
  - `.ai/raw_claude_cli_full/todos/*.json`
- Project paste/debug material found via search:
  - `.ai/raw_claude_cli_full/paste-cache/*.txt`
  - `.ai/raw_claude_cli_full/debug/*.txt`
- Partial historical code snapshots:
  - `.ai/raw_claude_cli_full/file-history/81355c0a-7e24-4e56-8a0f-858057b88744/*`
  - `.ai/raw_claude_cli_full/file-history/d831ed33-5cde-4a81-aeb4-d0fb1373d479/*`
  - `.ai/raw_claude_cli_full/file-history/fa3a102e-31e9-43cc-9ca1-241f470d576e/*`
  - `.ai/raw_claude_cli_full/file-history/8e8e82d3-94b2-45a4-8614-5f81ec78ed33/*`
  - `.ai/raw_claude_cli_full/file-history/8184253d-46f1-42b8-8830-fca860e6a5f5/*`

## Sources Classified As Mostly Noise
- Global Claude history outside this repo:
  - `.ai/raw_claude_cli_full/projects/C--Users-roeic/*.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--WINDOWS-system32/*.jsonl`
  - `.ai/raw_claude_cli_full/projects/C--/*.jsonl`
- Plugin and marketplace vendor content:
  - `.ai/raw_claude_cli_full/plugins/**`
- Generic environment or cache metadata:
  - `.ai/raw_claude_cli_full/settings.json`
  - `.ai/raw_claude_cli_full/settings.local.json`
  - `.ai/raw_claude_cli_full/stats-cache.json`
  - `.ai/raw_claude_cli_full/cache/changelog.md`
  - `.ai/raw_claude_cli_full/telemetry/**`
  - `.ai/raw_claude_cli_full/sessions/**`
  - `.ai/raw_claude_cli_full/shell-snapshots/**`

## How Conflicts Are Handled
- Prefer explicit project-session evidence over generic memory summaries.
- Prefer current source over stale todo state when a feature clearly exists in code.
- If a later session was interrupted by context or rate limits, mark the result as inferred instead of explicit.

## Current Known Uncertainty
- Several April sessions ended before final summaries were recorded.
- Some completed changes are visible in current source but were not explicitly confirmed in raw logs.
- `file-history` entries preserve useful snapshots but are not fully mapped back to exact filenames in this recovery pass.
- No clean final emulator or device verification transcript was recovered for the latest app state.
- The 2026-04-16 assign-task resource-linking fix was based on current source inspection and local build verification only; no additional raw archive files were scanned for that fix session.
- The 2026-04-16 second-pass UI refinement was also based on current source inspection and local build verification only; no new raw archive material was needed for that pass.
- The 2026-04-16 ParentDashboard redesign pass was likewise based on current source inspection and local build verification only.
- The 2026-04-16 whole-app redesign pass was likewise based on current source inspection and local build verification only; no new raw archive material was needed.
- The 2026-04-17 parent-side refinement pass also used current source inspection only; no new `.ai/raw_claude_cli_full` files were scanned.
- The 2026-04-17 child-side consistency/auth-feedback pass also used current source inspection only; no new `.ai/raw_claude_cli_full` files were scanned.
- The 2026-04-18 presentation-level UI pass also used current source inspection plus the connected Figma MCP `create_design_system_rules` response; no new `.ai/raw_claude_cli_full` files were scanned.
- External context used in that session:
  - Figma connector availability check via `whoami` only (no project file key was available in the repo to inspect actual frames)
  - focused web benchmarking against official Todoist, Google Tasks, Microsoft To Do, Cozi, and Android documentation pages

## Local Preservation Rule
- Keep `.ai/raw_claude_cli_full/` and `.ai/snapshots/` locally as permanent source material.
- These archives should stay ignored by Git unless the user explicitly asks to version them.
