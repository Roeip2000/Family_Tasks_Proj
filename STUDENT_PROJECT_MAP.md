# Student Project Map

## Packages
- `auth`: entry screen, parent login/register, QR login, and child selection.
- `parent`: parent dashboard, child management, QR generation, task assignment, and template management.
- `parent.adapter`: parent dashboard task adapter.
- `child`: child dashboard.
- `child.adapter`: child task adapter.
- `models`: `AssignedTask`, `ChildTask`, and `TaskTemplate`.
- `firebase`: Firebase singleton used by registration.
- `utils`: small shared helpers.

## Main Firebase Structure
- `/parents/{uid}`: parent profile root.
- `/parents/{uid}/children/{childId}`: child record.
- `/parents/{uid}/children/{childId}/tasks/{taskId}`: assigned child task.
- `/parents/{uid}/task_templates/{templateId}`: reusable task template.

## Current Models
- `AssignedTask`: parent dashboard display model for a task assigned to a child.
- `ChildTask`: child dashboard task model with id, title, due date, done status, image, and creation time.
- `TaskTemplate`: reusable template with id, title, and image.

## Simple Oral-Exam Story
- The parent owns the family data in Firebase.
- The parent creates children and templates.
- The parent assigns tasks to children.
- The child enters through QR/manual selection.
- The child dashboard loads tasks for the selected child.
- Pressing done updates only the task status.

## Removed Complexity
- No dashboard logout buttons.
- No automatic child login after app restart.
- No reward-counter feature.
