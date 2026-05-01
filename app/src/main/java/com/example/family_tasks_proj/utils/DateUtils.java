package com.example.family_tasks_proj.utils;

import java.util.Calendar;

// עוזר לנו לחשב תאריכים ודחיפות של משימות
public final class DateUtils {

    private DateUtils() {}

    // מחזיר כמה ימים נשארו עד התאריך שקיבלנו (או מספר שלילי אם התאריך עבר)
    public static long daysLeft(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return Long.MAX_VALUE;
        }

        String[] parts = dateStr.split("/");
        if (parts.length != 3) {
            return Long.MAX_VALUE;
        }

        try {
            int d = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            
            // תאריך המשימה
            Calendar calTask = Calendar.getInstance();
            calTask.set(y, m - 1, d, 0, 0, 0);
            calTask.set(Calendar.MILLISECOND, 0);

            // היום הנוכחי
            Calendar calToday = Calendar.getInstance();
            calToday.set(Calendar.HOUR_OF_DAY, 0);
            calToday.set(Calendar.MINUTE, 0);
            calToday.set(Calendar.SECOND, 0);
            calToday.set(Calendar.MILLISECOND, 0);

            // חישוב ההפרש
            long diff = calTask.getTimeInMillis() - calToday.getTimeInMillis();
            return diff / (24L * 60L * 60L * 1000L);
            
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    public static boolean isDueSoon(String date) {
        // בפרויקט הזה "דחוף" אומר שהתאריך הוא היום, מחר או עוד יומיים.
        long days = daysLeft(date);
        return days >= 0 && days <= 2;
    }

    public static boolean isOverdue(String date) {
        return daysLeft(date) < 0;
    }
}
