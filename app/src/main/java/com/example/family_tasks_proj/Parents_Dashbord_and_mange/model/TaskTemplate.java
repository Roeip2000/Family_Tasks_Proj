package com.example.family_tasks_proj.Parents_Dashbord_and_mange.model;

/**
 * מודל תבנית משימה — נשמר ב-Firebase.
 *
 * נתיב ב-DB: /parents/{uid}/task_templates/{id}
 * שדות: id (מזהה ייחודי), title (כותרת), imageBase64 (תמונה מקודדת),
 *      starsWorth (כמה כוכבים שווה משימה שתיווצר מהתבנית).
 *
 * הערה: constructor ריק חובה ל-Firebase deserialization.
 * המחלקה משמשת ישירות במסכי ניהול התבניות והקצאת המשימה,
 * כדי לשמור על קוד פשוט וברור בלי לעבוד עם HashMap ידני.
 */
public class TaskTemplate
{
    /** ערך ברירת מחדל היסטורי — תבניות ישנות שנשמרו לפני הוספת השדה יקבלו זאת. */
    public static final int DEFAULT_STARS_WORTH = 10;

    public String id;
    public String title;
    public String imageBase64;
    /** כמה כוכבים שווה משימה שנוצרת מהתבנית. מינימום 1. */
    public int starsWorth = DEFAULT_STARS_WORTH;

    /** constructor ריק — חובה ל-Firebase. */
    public TaskTemplate() {}

    public TaskTemplate(String id, String title, String imageBase64, int starsWorth)
    {
        this.id = id;
        this.title = title;
        this.imageBase64 = imageBase64;
        this.starsWorth = starsWorth;
    }

    public String toDisplayTitle() {
        if (title == null) {
            return "";
        }
        return title.trim();
    }

    /** מחזיר ערך כוכבים שמובטח חוקי (תיקון דאטה ישן בלי שדה או עם ערך לא חוקי). */
    public int safeStarsWorth() {
        if (starsWorth > 0) {
            return starsWorth;
        }
        return DEFAULT_STARS_WORTH;
    }

    @Override
    public String toString() {
        return toDisplayTitle();
    }
}
