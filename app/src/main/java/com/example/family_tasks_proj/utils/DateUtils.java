package com.example.family_tasks_proj.utils;

import java.util.Calendar;

// מחלקת עזר לבדיקת מצב תאריך של משימה
public class DateUtils {

    private static final int URGENT_DAYS = 2;

    public static long getDaysLeft(String dueDate) {
        // תאריך ריק מוחזר כמספר גדול כדי שלא ייחשב דחוף או באיחור
        if (dueDate == null || dueDate.trim().isEmpty()) {
            return 999;
        }

        try {
            // פירוק התאריך לפי פורמט יום/חודש/שנה
            String[] parts = dueDate.split("/");

            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1; // ב-Java חודשים מתחילים מ-0
            int year = Integer.parseInt(parts[2]);

            Calendar today = Calendar.getInstance();
            Calendar taskDate = Calendar.getInstance();

            // איפוס שעה כדי להשוות לפי יום בלבד
            clearTime(today);
            clearTime(taskDate);

            taskDate.set(year, month, day);

            // חישוב ההפרש בימים
            long difference = taskDate.getTimeInMillis() - today.getTimeInMillis();
            return difference / (1000 * 60 * 60 * 24);

        } catch (Exception exception) {
            return 999;
        }
    }

    public static boolean isDueSoon(String dueDate) {
        long daysLeft = getDaysLeft(dueDate);
        // משימה דחופה אם נשארו עד יומיים (0, 1 או 2)
        return daysLeft >= 0 && daysLeft <= URGENT_DAYS;
    }

    public static boolean isOverdue(String dueDate) {
        // משימה באיחור אם מספר הימים שנותרו הוא שלילי
        return getDaysLeft(dueDate) < 0;
    }

    // איפוס השעה ב-Calendar כדי לבצע השוואה נקייה בין תאריכים
    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
