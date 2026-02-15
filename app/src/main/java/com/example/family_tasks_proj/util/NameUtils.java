package com.example.family_tasks_proj.util;

/**
 * עוזר לבניית שם מלא — מרכז לוגיקה שחוזרת ב-4+ מקומות בפרויקט.
 * null-safe — מטפל בכל מצב של null/ריק.
 */
public final class NameUtils {

    private NameUtils() {} // לא ליצור instance

    /**
     * בונה שם מלא משם פרטי + שם משפחה.
     *
     * @param first שם פרטי (יכול להיות null)
     * @param last  שם משפחה (יכול להיות null)
     * @return שם מלא, או null אם שניהם ריקים/null
     */
    public static String fullName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.trim().isEmpty()) sb.append(first.trim());
        if (last != null && !last.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(last.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * בונה שם מלא עם fallback — אם אין שם, מחזיר את ברירת המחדל.
     *
     * @param first    שם פרטי
     * @param last     שם משפחה
     * @param fallback ערך ברירת מחדל (למשל childId)
     * @return שם מלא או fallback
     */
    public static String fullNameOrDefault(String first, String last, String fallback) {
        String name = fullName(first, last);
        return name != null ? name : fallback;
    }
}
