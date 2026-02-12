package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

/**
 * מודל תבנית משימה — נשמר ב-Firebase.
 *
 * נתיב ב-DB: /parents/{uid}/task_templates/{id}
 * שדות: id (מזהה ייחודי), title (כותרת), imageBase64 (תמונה מקודדת).
 *
 * הערה: constructor ריק חובה ל-Firebase deserialization.
 *
 * הערה: כרגע המחלקה הזו לא בשימוש ישיר — התבניות נשמרות כ-HashMap.
 *        ניתן להשתמש בה בעתיד כדי לקרוא תבניות עם DataSnapshot.getValue(TaskTemplate.class).
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
}
