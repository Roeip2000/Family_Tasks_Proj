package com.example.family_tasks_proj.child;

/**
 * מודל נתונים של ילד.
 *
 * נתיב ב-Firebase: /parents/{uid}/children/{childId}
 * שדות: firstName, lastName, profileImageBase64.
 *
 * constructor ריק חובה ל-DataSnapshot.getValue(Child.class).
 */
public class Child {
    public String firstName;
    public String lastName;
    /** תמונת הפרופיל של הילד — מחרוזת Base64 של JPEG. יכולה להיות null אם לא הוגדרה. */
    public String profileImageBase64;

    /** constructor ריק — חובה ל-Firebase deserialization. */
    public Child() {
    }

    /** constructor בסיסי — בלי תמונה */
    public Child(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImageBase64 = null;
    }

    /** constructor מלא — עם תמונת פרופיל */
    public Child(String firstName, String lastName, String profileImageBase64) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImageBase64 = profileImageBase64;
    }
}
