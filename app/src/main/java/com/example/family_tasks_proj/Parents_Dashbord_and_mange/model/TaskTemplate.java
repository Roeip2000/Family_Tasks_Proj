package com.example.family_tasks_proj.Parents_Dashbord_and_mange.model;

/**
 * מודל תבנית משימה — נשמר ב-Firebase.
 *
 * נתיב ב-DB: /parents/{uid}/task_templates/{id}
 * שדות: id (מזהה ייחודי), title (כותרת), imageBase64 (תמונה מקודדת).
 *
 * הערה: constructor ריק חובה ל-Firebase deserialization.
 * המחלקה משמשת ישירות במסכי ניהול התבניות והקצאת המשימה,
 * כדי לשמור על קוד פשוט וברור בלי לעבוד עם HashMap ידני.
 */
public class TaskTemplate {
    public String id;
    public String title;
    public String imageBase64;

    /** constructor ריק — חובה ל-Firebase. */
    public TaskTemplate()
    {
    }

    public TaskTemplate(String id, String title, String imageBase64)
    {
        this.id = id;
        this.title = title;
        this.imageBase64 = imageBase64;
    }

    public String toDisplayTitle() {
        return title == null ? "" : title.trim();
    }

    @Override
    public String toString() {
        return toDisplayTitle();
    }
}
