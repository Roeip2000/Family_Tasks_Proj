package com.example.family_tasks_proj.child.model;

/** מודל המייצג משימה שהוקצתה לילד. כוללת תאריך יעד, סטטוס ביצוע וכמות כוכבים. */
public class ChildTask {
    private String id;
    private String title;
    private String dueAt;
    private boolean isDone;
    private long starsWorth;
    private String imageBase64;
    private long createdAt;

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

    public long getStarsWorth() {
        return starsWorth;
    }

    public void setStarsWorth(long starsWorth) {
        this.starsWorth = starsWorth;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
