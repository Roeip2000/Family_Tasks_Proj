package com.example.family_tasks_proj.child.Class_child;

/**
 * מודל נתונים של ילד.
 * נתיב ב-Firebase: /parents/{uid}/children/{childId}
 *
 * constructor ריק חובה ל-DataSnapshot.getValue(Child.class).
 */
public class Child {
    public String firstName;
    public String lastName;

    public Child() {
    }

    public Child(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
