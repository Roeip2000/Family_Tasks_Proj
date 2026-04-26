package com.example.family_tasks_proj.child.model;

/**
 * מודל ילד — נשמר ב-Firebase תחת /parents/{uid}/children/{childId}.
 *
 * שדות: firstName, lastName, profileImageBase64.
 * פעולה בונה ריקה חובה ל-DataSnapshot.getValue(Child.class).
 */
public class Child {
    private String firstName;
    private String lastName;
    /** תמונת פרופיל מקודדת ב-Base64 (JPEG), או null אם לא נבחרה */
    private String profileImageBase64;

    /** פעולה בונה ריקה — חובה ל-Firebase. */
    public Child() {}

    /** פעולה בונה מלאה — משמשת ב-ManageChildrenActivity בעת הוספת ילד חדש. */
    public Child(String firstName, String lastName, String profileImageBase64) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImageBase64 = profileImageBase64;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }
}
