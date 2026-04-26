package com.example.family_tasks_proj.FireBase;

import java.util.HashMap;
import java.util.Map;

/** מודל נתונים של הורה כפי שנשמר ב-Firebase. */
public class ParentInFb {

    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Map<String, Object> children;
    /** תמונת הפרופיל של ההורה — מחרוזת Base64 של JPEG */
    private String profileImageBase64;

    /** פעולה בונה ריקה — חובה כדי ש-Firebase יוכל לקרוא את האובייקט. */
    public ParentInFb() {}

    // יוצר פרופיל הורה חדש עם role = "parent" ורשימת ילדים ריקה
    public ParentInFb(String uid, String firstName, String lastName, String email) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = "parent";
        this.children = new HashMap<>();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "ParentInFb{" + "uid='" + uid + '\'' + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + ", email='" + email + '\'' + ", role='" + role + '\'' + '}';
    }

    public Map<String, Object> getChildren() { return children; }
    public void setChildren(Map<String, Object> children) { this.children = children; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }
}
