package com.example.family_tasks_proj.child.Class_child;

/**
 * מודל משימה מצד הילד — נקרא מ-Firebase ב-ChildDashboardActivity.
 *
 * נתיב ב-DB: /parents/{uid}/children/{childId}/tasks/{taskId}
 * שדות: id, title, dueAt, isDone, starsWorth, imageBase64, createdAt.
 *
 * constructor ריק חובה ל-DataSnapshot.getValue(ChildTask.class).
 *
 * ===== הערות =====
 * - dueAt בפורמט "d/M/yyyy" (למשל "15/3/2026"). נקבע ע"י DatePicker ב-AssignTaskToChildActivity.
 * - starsWorth: כמות כוכבים שהילד מקבל כשמשלים את המשימה.
 * - isDone: סטטוס — true אם הילד סיים.
 * - imageBase64: תמונת המשימה מקודדת — מגיעה מתבנית שנבחרה ע"י ההורה.
 * - createdAt: חותמת זמן (epoch ms) של יצירת המשימה.
 */
public class ChildTask {
    public String id;
    public String title;
    /** תאריך יעד בפורמט "d/M/yyyy" */
    public String dueAt;
    public boolean isDone;
    public long starsWorth;
    /** תמונת המשימה מקודדת ב-Base64 (JPEG) */
    public String imageBase64;
    /** חותמת זמן יצירת המשימה (epoch milliseconds) */
    public long createdAt;

    /** constructor ריק — חובה ל-Firebase. */
    public ChildTask() {}
}
