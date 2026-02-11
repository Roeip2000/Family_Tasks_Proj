package com.example.family_tasks_proj.FireBase;

/**
 * מודל נתונים של הורה כפי שנשמר ב-Firebase Realtime Database.
 *
 * נתיב ב-DB: /parents/{uid}
 * שדות: uid, firstName, lastName, email, role, children.
 *
 * הערה: חובה לשמור על constructor ריק + getters/setters
 *        כדי ש-Firebase DataSnapshot.getValue() יעבוד.
 */
public class ParentInFb {

    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private java.util.Map<String, Object> children;

    /** constructor ריק — חובה ל-Firebase deserialization. */
    public ParentInFb()
    {
    }

    /**
     * יוצר פרופיל הורה חדש עם role = "parent" ורשימת ילדים ריקה.
     *
     * @param uid       Firebase Auth UID
     * @param firstName שם פרטי
     * @param lastName  שם משפחה
     * @param email     אימייל
     */
    public ParentInFb(String uid, String firstName, String lastName, String email)
    {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = "parent";
        this.children = new java.util.HashMap<>();
    }

    // Getters & Setters — חובה ל-Firebase serialization



    public String getUid()
    {
        return uid;
    }

    public void setUid(String uid)
    {
        this.uid = uid;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {

        this.lastName = lastName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    @Override
    public String toString()
    {
        return "ParentInFb{" + "uid='" + uid + '\'' + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + ", email='" + email + '\'' + ", role='" + role + '\'' + '}';
    }
    public java.util.Map<String, Object> getChildren()
    {
        return children;
    }


    public void setChildren(java.util.Map<String, Object> children)
    {
        this.children = children;
    }
}
