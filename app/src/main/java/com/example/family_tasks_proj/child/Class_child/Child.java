package com.example.family_tasks_proj.child.Class_child;

/**
 * מודל נתונים של ילד.
 *
 * נתיב ב-Firebase: /parents/{uid}/children/{childId}
 * שדות: firstName (שם פרטי), lastName (שם משפחה).
 *
 * constructor ריק חובה ל-DataSnapshot.getValue(Child.class).
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף שדה stars (int) — כמות כוכבים שהילד צבר.
 * TODO: להוסיף encapsulation (private fields + getters/setters).
 */
public class Child {
    public String firstName;
    public String lastName;

    /** constructor ריק — חובה ל-Firebase deserialization. */
    public Child() {
    }

    public Child(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
