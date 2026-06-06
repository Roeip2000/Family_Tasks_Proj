# Cleanup Report — Family Tasks

**Branch:** `cleanup/unused`
**Date:** 2026-06-06
**Scope:** Safe dead-code / dead-resource removal only. No behavior, UI, flow,
Firebase path, or field-name changes. "Unused" = **zero** references across
Java + XML + AndroidManifest.xml, proven by search before any removal.

## Executive summary

After a full declared-vs-referenced audit of every color, layout, drawable,
style, dimen, string, and Java member, **nothing qualified for safe removal.**
Every declared resource and every Java method/field/import has at least one
real reference. The previous commit (`6d79d1a "ניקוי פרוייקט אחרון"` — *final
project cleanup*) had already removed the dead code, so the tree was already
clean when this pass started.

This was verified two independent ways:
1. **Manual reference search** for every declared item (recipes below).
2. **`./gradlew lintDebug`** — the report contains **0 `UnusedResources`
   findings** (15 unrelated warnings: `OldTargetApi`, `GradleDependency`,
   `Overdraw`, `ButtonStyle`, `NotifyDataSetChanged`, `DiscouragedApi`).

Because there was nothing to delete, **no code/resource commits were made** for
the three categories. Making cosmetic deletions just to fill the categories
would risk breaking a graded project, which the constraints forbid. This report
is the only added file.

### Totals removed

| Category | Removed |
|---|---|
| Colors | 0 |
| Layouts | 0 |
| Drawables | 0 |
| Styles | 0 |
| Dimens | 0 |
| Strings | 0 |
| Methods | 0 |
| Imports | 0 |
| Fields | 0 |
| **Lines saved** | **0** |

---

## Table 1 — Colors (`res/values/colors.xml`)

22 colors declared, **22 referenced → 0 removable.** Search recipe per name:
`@color/NAME` across `res/**` + manifest, and `R.color.NAME` across `*.java`.

| Color | Verdict | Example reference found |
|---|---|---|
| white | KEEP | themes.xml, activity_manage_children.xml |
| primary | KEEP | themes.xml, bg_button_primary.xml, many layouts |
| primary_light | KEEP | themes.xml, fragment_child_q_r_login.xml |
| primary_dark | KEEP | ic_camera.xml, item_parent_task.xml, item_child_task.xml |
| secondary | KEEP | themes.xml |
| secondary_light | KEEP | themes.xml |
| bg_screen | KEEP | activity_main.xml, activity_child_dashboard.xml |
| bg_card | KEEP | bg_preview_shell.xml, bg_spinner.xml, item_parent_task.xml |
| surface_muted | KEEP | bg_preview_shell.xml, activity_parent_dashboard.xml |
| hero_top | KEEP | bg_repository_list.xml |
| text_primary | KEEP | styles.xml, themes.xml, R.color (Java) |
| text_secondary | KEEP | many layouts, `R.color.text_secondary` (Java x2) |
| text_hint | KEEP | bg_status_dot.xml, ic_image_placeholder.xml |
| text_inverse | KEEP | styles.xml, ic_check.xml |
| border_light | KEEP | bg_preview_shell.xml, bg_spinner.xml |
| border_strong | KEEP | activity_parent_dashboard.xml |
| divider | KEEP | bg_repository_list.xml, activity_manage_children.xml |
| divider_light | KEEP | fragment_child_q_r_login.xml, item_child_task.xml |
| success | KEEP | activity_parent_dashboard.xml |
| error | KEEP | themes.xml |
| danger | KEEP | `R.color.danger` (Java x2), activity_parent_dashboard.xml |
| urgent | KEEP | `R.color.urgent` (Java x2), activity_parent_dashboard.xml |

*Note: the `md_theme_*` Material3 colors mentioned in the brief do not exist in
this project's `colors.xml`, so there were none to evaluate.*

---

## Table 2 — Designs (layouts / drawables / styles / dimens / strings)

### Layouts (15 declared → 15 referenced, 0 removable)
Every layout is inflated from Java via `R.layout.<name>`
(`setContentView` / `inflate` / adapter). Proven with
`R.layout.NAME` across `*.java`:

`activity_main`, `activity_child_selection`, `activity_child_dashboard`,
`activity_parent_dashboard`, `activity_manage_children`,
`activity_parent_task_template`, `activity_assign_task_to_child`,
`activity_generate_qr`, `fragment_parent_login`, `fragment_parent_register`,
`fragment_child_q_r_login`, `item_parent_task`, `item_child_task`,
`item_task_template`, `item_manage_child` — all KEEP.

### Drawables (11 declared → 11 referenced, 0 removable)
Search: `@drawable/NAME` (xml) + `R.drawable.NAME` (java).

| Drawable | Verdict | Reference |
|---|---|---|
| bg_button_primary | KEEP | styles.xml (Widget.Family.PrimaryButton) |
| bg_preview_shell | KEEP | item_parent_task.xml |
| bg_repository_list | KEEP | fragment_parent_register/login, manage_children, … |
| bg_screen_gradient | KEEP | fragment_child_q_r_login.xml |
| bg_spinner | KEEP | activity_child_selection.xml, activity_assign_task_to_child.xml |
| bg_status_dot | KEEP | item_parent_task.xml |
| ic_camera | KEEP | fragment_child_q_r_login.xml |
| ic_check | KEEP | item_child_task.xml |
| ic_image_placeholder | KEEP | item_task_template.xml |
| ic_launcher_background | KEEP | mipmap-anydpi-v26/ic_launcher(_round).xml |
| ic_launcher_foreground | KEEP | mipmap-anydpi-v26/ic_launcher(_round).xml |

### Styles (0 removable)
`Theme.Family_Tasks_Proj` (manifest), `Base.Theme.Family_Tasks_Proj` (parent of
the theme), and the four `Widget.Family.*` styles
(`ScreenTitle`, `ScreenSubtitle`, `HelperText`, `PrimaryButton`) are all
referenced (`fragment_child_q_r_login.xml`). `Widget.Material3.*` styles belong
to the library, not the project. The two empty anchor styles `Widget` and
`Widget.Family` are kept — see **NEEDS REVIEW**.

### Dimens (6 declared → 6 referenced, 0 removable)
Search: `@dimen/NAME` + `R.dimen.NAME`.

| Dimen | Reference |
|---|---|
| card_corner_radius | fragment_child_q_r_login.xml |
| button_height | styles.xml, fragment_child_q_r_login.xml |
| button_corner_radius | bg_button_primary.xml |
| screen_padding | fragment_child_q_r_login.xml |
| auth_fragment_container_height | activity_main.xml |
| text_title | fragment_child_q_r_login.xml |

### Strings (86 declared → 86 referenced, 0 removable)
Every `<string>` resolves to at least one `@string/NAME` (XML / manifest) or
`R.string.NAME` (Java). `app_name` is used by the manifest and
`activity_main.xml`. No `string-array` resources exist. → all KEEP.

---

## Table 3 — Functions (methods / fields / imports / dead code)

Audited all 18 `.java` files.

- **Unused imports:** none. Every `import` in every file resolves to a symbol
  used in that file (verified per file).
- **Unused private methods:** none. e.g. `MainActivity.showFragment` (called in
  `onCreate`/`onClick`), `DateUtils.clearTime` (called in `getDaysLeft`),
  `ParentDashboardTaskAdapter.showTaskImage` (called in `onBindViewHolder`),
  every `load*/save*/assign*/render*/bind*` helper has a call site.
- **Unused public methods:** none removable. `DateUtils.getDaysLeft` is public
  but is called by `isDueSoon`/`isOverdue`
  (`rg "getDaysLeft\s*\(" app/src/main` → 3 hits), so it stays.
- **Unused fields / locals:** none. Every Activity field is assigned via
  `findViewById` and then read (listener wiring / data binding).
- **Dead/unreachable code:** none. No `if (false)`, no commented-out blocks, no
  leftover debug logs. Empty `onCancelled`/`onNothingSelected` bodies are
  required framework callbacks and are kept.
- **Firebase model classes** (`ChildTask`, `TaskTemplate`, `AssignedTask`):
  all getters/setters/fields kept — they are the DB contract and are read/written
  by Firebase reflection (`getValue(*.class)` / `setValue(obj)`).

---

## Table 4 — NEEDS REVIEW (intentionally NOT changed)

| Item | File | Why left alone |
|---|---|---|
| `Widget` (empty style) | values/styles.xml | Implicit parent anchor of the `Widget.Family.*` chain. Not directly `@style/`-referenced, so technically deletable, but it is part of the documented style hierarchy and removing it yields ~0 benefit while touching the inheritance chain. Kept to honor the "behavior must stay identical / be conservative" rule. |
| `Widget.Family` (empty style) | values/styles.xml | Same as above — implicit parent of `Widget.Family.ScreenTitle/Subtitle/HelperText/PrimaryButton`. |
| `DateUtils.getDaysLeft` (public) | utils/DateUtils.java | Public but only called internally. It is **used**, so not removed. Could *optionally* be narrowed to `private`, but that is a refactor, not a dead-code removal, and is out of scope. |
| `targetSdk`/dependency/overdraw lint warnings | build.gradle.kts, layouts | 15 pre-existing lint warnings unrelated to dead code. Out of scope (no version/library/UI changes allowed). |

---

## Final verification

- ✅ `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (baseline and unchanged tree;
  no code was modified).
- ✅ `./gradlew lintDebug` → **0 errors, 15 warnings, 0 `UnusedResources`**
  (no new findings — the tree is unchanged).
- ✅ No broken references introduced (nothing was deleted).
- ✅ Unchanged flows confirmed intact (no code touched): parent register/login,
  add child, create template, assign task, show/scan QR, child dashboard task
  load, mark task done.

**Conclusion:** the project is already free of unused colors, designs, and
functions. No safe removals exist; the safest correct action is to change
nothing and document the proof.
