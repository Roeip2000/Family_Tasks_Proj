package com.example.family_tasks_proj.child.model;

/**
 * מודל ילד — נשמר ב-Firebase תחת /parents/{uid}/children/{childId}.
 *
 * שדות: firstName, lastName, profileImageBase64.
 * constructor ריק חובה ל-DataSnapshot.getValue(Child.class).
 */
public class Child {
    public String firstName;
    public String lastName;
    /** תמונת פרופיל מקודדת ב-Base64 (JPEG), או null אם לא נבחרה */
    public String profileImageBase64;

    /** constructor ריק — חובה ל-Firebase. */
    public Child() {}

    /** constructor מלא — משמש ב-ManageChildrenActivity בעת הוספת ילד חדש. */
    public Child(String firstName, String lastName, String profileImageBase64) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImageBase64 = profileImageBase64;
    }
}
