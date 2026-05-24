package com.example.family_tasks_proj.models;

/** מודל המייצג משימה שהוקצתה לילד. כולל תאריך יעד וסטטוס ביצוע. */
public class ChildTask {
    private String title;
    private String dueAt;
    private String imageBase64;
    private boolean isDone;

    public ChildTask() {
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

    // המימוש נמצא במחלקה ChildTask.
    // אין קריאה ישירה מאקטיביטי; Firebase משתמש בו בעקיפין
    // מ-AssignTaskToChildActivity בזמן newTaskReference.setValue(newTask).
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
