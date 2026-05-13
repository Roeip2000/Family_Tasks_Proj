# Project Presentation Guide

## Short Project Summary
Family Tasks is an Android app for parent-managed family tasks. A parent creates children, creates reusable task templates, assigns tasks, and reviews task progress. A child enters through QR/manual selection, sees assigned tasks, and marks tasks as done.

## Demo Flow
1. Open the app on the home screen.
2. Log in or register as a parent.
3. Add or edit children.
4. Create a task template with a title and optional image.
5. Assign a task to a child with a due date.
6. Open the child flow through QR/manual entry.
7. Select the child and open the child dashboard.
8. Mark a task as done.
9. Return to the parent dashboard and show the updated task status.

## What To Explain In The Oral Exam
- Firebase Auth identifies the parent.
- Realtime Database stores parent data under `/parents/{uid}`.
- Children are stored under `/parents/{uid}/children/{childId}`.
- Tasks are stored under `/parents/{uid}/children/{childId}/tasks/{taskId}`.
- Templates are stored under `/parents/{uid}/task_templates/{templateId}`.
- The QR flow identifies the family and then opens child selection.
- The child dashboard receives ids through Intent extras and loads data from Firebase.
- Marking a task done updates the task `isDone` field.

## Current Main Classes
- `MainActivity`: entry screen and role selection.
- `ParentLoginFragment`: parent login.
- `ParentRegisterFragment`: parent registration.
- `ChildQRLoginFragment`: QR parsing and parent validation.
- `ChildSelectionActivity`: parent/child selection and child dashboard handoff.
- `ParentDashboardActivity`: parent task overview and quick actions.
- `ManageChildrenActivity`: add, edit, and delete children.
- `GenerateQRActivity`: show the family QR code.
- `AssignTaskToChildActivity`: choose child, template, due date, and create a task.
- `ParentTaskTemplateActivity`: manage reusable templates.
- `ChildDashboardActivity`: load child tasks and mark tasks done.
- `ChildTaskAdapter`: display child task cards.
- `ParentDashboardTaskAdapter`: display parent task cards.
- `AssignedTask`, `ChildTask`, `TaskTemplate`: simple data models.
- `ImageHelper`, `DateUtils`, `NameUtils`, `ListUtils`: small shared helpers.

## Removed Complexity
- No dashboard logout buttons.
- No automatic child login after reopening the app.
- No reward-counter fields or UI.
