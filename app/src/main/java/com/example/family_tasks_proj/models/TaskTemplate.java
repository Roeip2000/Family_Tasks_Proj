package com.example.family_tasks_proj.models;

/** מודל המייצג תבנית של משימה (למשל "סידור חדר"). משמש ליצירה מהירה של משימות חוזרות. */
public class TaskTemplate {
    private String title;
    private String imageBase64;

    public TaskTemplate() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
