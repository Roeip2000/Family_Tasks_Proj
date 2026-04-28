package com.example.family_tasks_proj.util;

import java.util.Calendar;

/** מחלקת עזר לטיפול בתאריכים וחישוב דחיפות משימות. */
public final class DateUtils {

    private DateUtils() {
    }

    // מחשב כמה ימים נותרו עד תאריך היעד (מחזיר ערך שלילי אם התאריך עבר)
    public static long daysLeft(String dueAt) {
        if (dueAt == null || dueAt.trim().isEmpty()) {
            return Long.MAX_VALUE;
        }
        String[] parts = dueAt.trim().split("/");
        if (parts.length != 3) {
            return Long.MAX_VALUE;
        }

        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            Calendar due = Calendar.getInstance();
            due.set(year, month - 1, day, 0, 0, 0);
            due.set(Calendar.MILLISECOND, 0);

            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);

            return (due.getTimeInMillis() - now.getTimeInMillis()) / (24L * 60L * 60L * 1000L);
        } catch (Exception ignored) {
            return Long.MAX_VALUE;
        }
    }

    // משימה דחופה מוגדרת ככזו שנותרו לה עד יומיים לביצוע
    public static boolean isDueSoon(String dueAt) {
        long days = daysLeft(dueAt);
        return days >= 0 && days <= 2;
    }

    // משימה באיחור אם תאריך היעד שלה קטן מהיום
    public static boolean isOverdue(String dueAt) {
        return daysLeft(dueAt) < 0;
    }
}
