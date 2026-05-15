package com.example.family_tasks_proj.models;

/** מודל המייצג משימה שהוקצתה לילד. כולל תאריך יעד וסטטוס ביצוע. */
public class ChildTask {
    private String id;
    private String title;
    private String dueAt;
    private boolean isDone;
    private String imageBase64;

    public ChildTask() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDueAt() {
        return dueAt;
    }

    public void setDueAt(String dueAt) {
        this.dueAt = dueAt;
    }

    public boolean getIsDone() {
        return isDone;
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
