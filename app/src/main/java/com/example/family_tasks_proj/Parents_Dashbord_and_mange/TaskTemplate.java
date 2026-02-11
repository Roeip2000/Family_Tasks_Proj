package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

/**
 * מודל תבנית משימה (לא בשימוש כרגע — התבניות נשמרות כ-HashMap).
 *
 * נתיב ב-DB: /parents/{uid}/task_templates/{id}
 * constructor ריק חובה ל-Firebase deserialization.
 */
public class TaskTemplate {
    public String id;
    public String title;
    public String imageBase64;

    public TaskTemplate()
    {
    }

    public TaskTemplate(String id, String title, String imageBase64)
    {
        this.id = id;
        this.title = title;
        this.imageBase64 = imageBase64;
    }
}













