package com.example.family_tasks_proj.util;

import java.util.Calendar;

/**
 * עוזר לחישובי תאריכים — מרכז לוגיקה שחוזרת ב-ChildDashboardActivity ו-ChildTaskAdapter.
 * כל המתודות סטטיות ו-null-safe.
 */
public final class DateUtils {

    private DateUtils() {} // לא ליצור instance

    /**
     * מחשב כמה ימים נותרו עד תאריך היעד.
     *
     * @param dueAt תאריך בפורמט "d/M/yyyy"
     * @return מספר ימים (שלילי = עבר, 0 = היום), או Long.MAX_VALUE אם הפורמט לא תקין
     */
    public static long daysLeft(String dueAt) {
        if (dueAt == null || dueAt.trim().isEmpty()) return Long.MAX_VALUE;
        String[] parts = dueAt.trim().split("/");
        if (parts.length != 3) return Long.MAX_VALUE;

        try {
            int d = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);

            Calendar due = Calendar.getInstance();
            due.set(y, m - 1, d, 0, 0, 0);
            due.set(Calendar.MILLISECOND, 0);

            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);

            return (due.getTimeInMillis() - now.getTimeInMillis()) / (24L * 60L * 60L * 1000L);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * האם המשימה דחופה (0-2 ימים)?
     *
     * @param dueAt תאריך בפורמט "d/M/yyyy"
     * @return true אם נשארו 0-2 ימים
     */
    public static boolean isDueSoon(String dueAt) {
        long days = daysLeft(dueAt);
        return days >= 0 && days <= 2;
    }
}
