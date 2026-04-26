package com.example.family_tasks_proj.child.model;

/**
 * מודל משימה מצד הילד — נקרא מ-Firebase ב-ChildDashboardActivity.
 *
 * נתיב במסד הנתונים: /parents/{uid}/children/{childId}/tasks/{taskId}
 * שדות: id, title, dueAt, isDone, starsWorth, imageBase64, createdAt.
 *
 * פעולה בונה ריקה חובה ל-DataSnapshot.getValue(ChildTask.class).
 *
 * ===== הערות =====
 * - dueAt בפורמט "d/M/yyyy" (למשל "15/3/2026"). נקבע ע"י DatePicker ב-AssignTaskToChildActivity.
 * - starsWorth: כמות כוכבים שהילד מקבל כשמשלים את המשימה.
 * - isDone: סטטוס — true אם הילד סיים.
 * - imageBase64: תמונת המשימה מקודדת — מגיעה מתבנית שנבחרה ע"י ההורה.
 * - createdAt: חותמת זמן במילישניות של יצירת המשימה.
 */
public class ChildTask {
    private String id;
    private String title;
    /** תאריך יעד בפורמט "d/M/yyyy" */
    private String dueAt;
    private boolean isDone;
    private long starsWorth;
    /** תמונת המשימה מקודדת ב-Base64 (JPEG) */
    private String imageBase64;
    /** חותמת זמן יצירת המשימה במילישניות */
    private long createdAt;

    /** פעולה בונה ריקה — חובה ל-Firebase. */
    public ChildTask() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDueAt() { return dueAt; }
    public void setDueAt(String dueAt) { this.dueAt = dueAt; }
    public boolean getIsDone() { return isDone; }
    public void setIsDone(boolean isDone) { this.isDone = isDone; }
    public long getStarsWorth() { return starsWorth; }
    public void setStarsWorth(long starsWorth) { this.starsWorth = starsWorth; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
