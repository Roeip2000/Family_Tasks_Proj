package com.example.family_tasks_proj.util;

import java.util.Calendar;

// עוזר לחישובי תאריכים במסכי המשימות
public final class DateUtils {

    private DateUtils() {} // לא ליצור מופע של המחלקה

    // מחשב כמה ימים נותרו עד תאריך היעד
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
        } catch (NumberFormatException exception) {
            return Long.MAX_VALUE;
        }
    }

    // משימה דחופה: 0-2 ימים (כאן משנים אם רוצים שדחוף יהיה 3 ימים)
    public static boolean isDueSoon(String dueAt) {
        long days = daysLeft(dueAt);
        return days >= 0 && days <= 2;
    }

    // משימה באיחור: עברה את תאריך היעד
    public static boolean isOverdue(String dueAt) {
        long days = daysLeft(dueAt);
        return days < 0;
    }
}
