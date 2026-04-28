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
            
            // יצירת אובייקט תאריך עבור יעד המשימה
            Calendar dueCalendar = Calendar.getInstance();
            dueCalendar.set(year, month - 1, day, 0, 0, 0);
            dueCalendar.set(Calendar.MILLISECOND, 0);

            // יצירת אובייקט תאריך עבור היום (עכשיו)
            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.set(Calendar.HOUR_OF_DAY, 0);
            nowCalendar.set(Calendar.MINUTE, 0);
            nowCalendar.set(Calendar.SECOND, 0);
            nowCalendar.set(Calendar.MILLISECOND, 0);

            // מחזיר את ההפרש בימים בין התאריכים
            return (dueCalendar.getTimeInMillis() - nowCalendar.getTimeInMillis()) / (24L * 60L * 60L * 1000L);
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
