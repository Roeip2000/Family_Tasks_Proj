# Structure Refactor Report

## Current Package Structure
- `auth`: entry screen, parent auth fragments, child QR fragment, child selection.
- `parent`: parent dashboard and parent-side task/child/template screens.
- `parent.adapter`: parent dashboard task adapter.
- `child`: child dashboard.
- `child.adapter`: child task adapter.
- `models`: shared data models.
- `firebase`: Firebase singleton used by registration.
- `utils`: small shared helper classes.

## Current Rule
Do not perform another broad package refactor late in the project. The current package map is already simple enough to explain in the oral exam.

## Cleanup Completed
- Removed older generated notes that referenced obsolete package names.
- Updated UML source files to the current package names.
- Kept only small helper classes that still have direct use.
