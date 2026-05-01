package com.example.family_tasks_proj.models;

/** מודל המייצג תבנית של משימה (למשל "סידור חדר"). משמש ליצירה מהירה של משימות חוזרות. */
public class TaskTemplate {
    public static final int DEFAULT_STARS_WORTH = 10;

    private String id;
    private String title;
    private String imageBase64;
    private int starsWorth = DEFAULT_STARS_WORTH;

    public TaskTemplate() {
    }

    public TaskTemplate(String id, String title, String imageBase64, int starsWorth) {
        this.id = id;
        this.title = title;
        this.imageBase64 = imageBase64;
        this.starsWorth = starsWorth;
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

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public int getStarsWorth() {
        return starsWorth;
    }

    public void setStarsWorth(int starsWorth) {
        this.starsWorth = starsWorth;
    }

    public int safeStarsWorth() {
        if (starsWorth > 0) {
            return starsWorth;
        }
        return DEFAULT_STARS_WORTH;
    }

    @Override
    public String toString() {
        if (title != null) {
            return title;
        }
        return "";
    }
}
