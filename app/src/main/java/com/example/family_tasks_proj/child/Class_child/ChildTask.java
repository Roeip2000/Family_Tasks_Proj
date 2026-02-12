package com.example.family_tasks_proj.child.Class_child;

/**
 * מודל משימה מצד הילד — נקרא מ-Firebase ב-ChildDashboardActivity.
 *
 * נתיב ב-DB: /parents/{uid}/children/{childId}/tasks/{taskId}
 * שדות: id, title, dueAt, isDone, starsWorth.
 *
 * constructor ריק חובה ל-DataSnapshot.getValue(ChildTask.class).
 *
 * ===== הערות =====
 * - dueAt בפורמט "d/M/yyyy" (למשל "15/3/2026"). נקבע ע"י DatePicker ב-AssignTaskToChildActivity.
 * - starsWorth: כמות כוכבים שהילד מקבל כשמשלים את המשימה.
 * - isDone: סטטוס — true אם הילד סיים.
 *
 * TODO: להוסיף שדה imageBase64 — התמונה שנשמרת עם המשימה.
 * TODO: להוסיף שדה createdAt — חותמת זמן ליצירת המשימה.
 */
public class ChildTask {
    public String id;
    public String title;
    /** תאריך יעד בפורמט "d/M/yyyy" */
    public String dueAt;
    public boolean isDone;
    public long starsWorth;

    /** constructor ריק — חובה ל-Firebase. */
    public ChildTask() {}
}
