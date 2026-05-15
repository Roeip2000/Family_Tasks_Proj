package com.example.family_tasks_proj.models;

/** מודל משימה מוקצית להצגה בדשבורד ההורה. כולל את פרטי הילד והמשימה. */
public class AssignedTask {
    private String childName;
    private String title;
    private String dueAt;
    private String imageBase64;
    private boolean isDone;

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
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

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public boolean getIsDone() {
        return isDone;
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }
}
