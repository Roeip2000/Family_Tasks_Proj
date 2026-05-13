# Code Explainability Report

## Current Shape
- The project is a simple Java Android app with Firebase Auth and Firebase Realtime Database.
- Parent screens handle login/register, child management, QR generation, task assignment, task templates, and dashboard review.
- Child screens handle QR/manual entry, child selection, task loading, filtering, and marking tasks as done.
- The app keeps Firebase paths direct and easy to explain under `/parents/{uid}`.

## Main Student-Explainable Points
- `MainActivity` starts from the role-selection home screen.
- Parent login/register uses Firebase Auth.
- Child entry passes `parentId` and `childId` through the current navigation flow.
- Task templates store a title and optional image.
- Assigned tasks store title, due date, done status, optional image, and creation time.
- Parent dashboard summaries are calculated from task status.
- Child dashboard shows open tasks and urgency filters.

## Removed Complexity
- Dashboard logout buttons were removed.
- Automatic reopen from old local child-login data was removed.
- Reward counters were removed from models, Firebase writes, layouts, colors, drawables, strings, and adapters.

## Verification
- `.\gradlew.bat lintDebug` completed with BUILD SUCCESSFUL.
- `.\gradlew.bat assembleDebug` completed with BUILD SUCCESSFUL.
