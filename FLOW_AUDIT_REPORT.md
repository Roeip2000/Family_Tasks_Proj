# Flow Audit Report

## Parent Flow
1. Parent opens `MainActivity`.
2. Parent chooses login or register.
3. Firebase Auth signs the parent in.
4. Parent dashboard loads children and tasks from `/parents/{uid}`.
5. Parent can manage children, templates, QR, and task assignment.

## Child QR Flow
1. Child opens the child entry card.
2. `ChildQRLoginFragment` scans or parses the QR payload.
3. The fragment validates the parent id in Firebase.
4. If the QR points to a family, the app opens `ChildSelectionActivity`.
5. If the QR includes a valid child id, the app can open `ChildDashboardActivity` directly.
6. The dashboard receives `parentId` and `childId` in Intent extras.

## Child Dashboard Flow
1. `ChildDashboardActivity` reads `parentId` and `childId` from Intent extras.
2. It loads the child profile from `/parents/{parentId}/children/{childId}`.
3. It loads tasks from `/parents/{parentId}/children/{childId}/tasks`.
4. It filters open, urgent, and overdue tasks.
5. When the child marks a task done, the app writes `isDone=true` for that task.

## Data Paths
- Parent profile: `/parents/{uid}`
- Children: `/parents/{uid}/children/{childId}`
- Child tasks: `/parents/{uid}/children/{childId}/tasks/{taskId}`
- Task templates: `/parents/{uid}/task_templates/{templateId}`

## Removed Flow Complexity
- The app no longer reopens dashboards automatically from old local child-login data.
- The dashboard logout flow was removed from parent and child screens.
- Reward-counter updates were removed from task completion.

## Verification
- Source search found no remaining app-source references to removed local child-login helpers, reward-counter ids, or logout button ids.
- `.\gradlew.bat lintDebug` completed with BUILD SUCCESSFUL.
- `.\gradlew.bat assembleDebug` completed with BUILD SUCCESSFUL.
