package com.example.family_tasks_proj.Parents_Dashbord_and_mange.model;

// מודל משימה מוקצית להצגה בדשבורד ההורה
public class AssignedTask {
    private String childId;
    private String childName;
    private String childProfileBase64;
    private String taskId;
    private String title;
    private String dueAt;
    private boolean isDone;

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }
    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }
    public String getChildProfileBase64() { return childProfileBase64; }
    public void setChildProfileBase64(String childProfileBase64) { this.childProfileBase64 = childProfileBase64; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDueAt() { return dueAt; }
    public void setDueAt(String dueAt) { this.dueAt = dueAt; }
    public boolean getIsDone() { return isDone; }
    public void setIsDone(boolean isDone) { this.isDone = isDone; }
}
