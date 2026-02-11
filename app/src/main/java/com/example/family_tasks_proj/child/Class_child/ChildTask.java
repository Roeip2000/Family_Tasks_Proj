package com.example.family_tasks_proj.child.Class_child;

/**
 * מודל משימה מצד הילד — נקרא מ-Firebase ב-ChildDashboardActivity.
 *
 * נתיב ב-DB: /parents/{uid}/children/{childId}/tasks/{taskId}
 * constructor ריק חובה ל-DataSnapshot.getValue(ChildTask.class).
 */
public class ChildTask {
    public String id;
    public String title;
    /** תאריך יעד בפורמט "d/M/yyyy" */
    public String dueAt;
    public boolean isDone;
    public long starsWorth;

    public ChildTask() {}
}
