package com.example.family_tasks_proj.util;

/** מחלקת עזר לטיפול בשמות משתמשים. */
public final class NameUtils {

    private NameUtils() {}

    // מחזיר שם מלא (פרטי + משפחה) או null אם שניהם ריקים
    public static String fullName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.trim().isEmpty()) sb.append(first.trim());
        if (last != null && !last.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(last.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // מחזיר שם מלא או ערך ברירת מחדל אם אין שם
    public static String fullNameOrDefault(String first, String last, String fallback) {
        String name = fullName(first, last);
        return name != null ? name : fallback;
    }
}
