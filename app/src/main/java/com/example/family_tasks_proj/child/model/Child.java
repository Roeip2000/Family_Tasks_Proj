package com.example.family_tasks_proj.child.model;

// מודל ילד כפי שנשמר תחת ההורה ב-Firebase
public class Child {
    private String firstName;
    private String lastName;
    private String profileImageBase64;

    // חובה ל-Firebase
    public Child() {}

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
