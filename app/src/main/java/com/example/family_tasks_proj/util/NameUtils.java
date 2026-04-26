package com.example.family_tasks_proj.util;

/** עוזר לבניית שם מלא בצורה בטוחה גם כשיש null או טקסט ריק. */
public final class NameUtils {

    private NameUtils() {} // לא ליצור מופע של המחלקה

    // בונה שם מלא משם פרטי ושם משפחה
    public static String fullName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.trim().isEmpty()) sb.append(first.trim());
        if (last != null && !last.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(last.trim());
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

    // בונה שם מלא, ואם אין שם מחזיר את ערך ברירת המחדל
    public static String fullNameOrDefault(String first, String last, String fallback) {
        String name = fullName(first, last);
        if (name != null) {
            return name;
        }
        return fallback;
    }
}
