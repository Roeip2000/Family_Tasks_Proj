package com.example.family_tasks_proj.utils;

import java.util.Calendar;

public class DateUtils {

    private static final int HOURS_IN_DAY = 24;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MILLIS_IN_SECOND = 1000;
    private static final int URGENT_THRESHOLD_DAYS = 2; // מספר הימים שמעליהם משימה נחשבת דחופה
    public static final long NO_VALID_DATE = Long.MAX_VALUE; // מספר מיוחד שמסמן שלא התקבל תאריך תקין

    // מחזיר כמה ימים נשארו עד התאריך שקיבלנו (או מספר שלילי אם התאריך עבר)
    public static long daysLeft(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return NO_VALID_DATE;
        }

        String[] parts = dateStr.split("/");
        if (parts.length != 3) {
            return NO_VALID_DATE;
        }

        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            
            Calendar calTask = Calendar.getInstance();
            calTask.set(year, month - 1, day, 0, 0, 0);
            calTask.set(Calendar.MILLISECOND, 0);

            Calendar calToday = Calendar.getInstance();
            calToday.set(Calendar.HOUR_OF_DAY, 0);
            calToday.set(Calendar.MINUTE, 0);
            calToday.set(Calendar.SECOND, 0);
            calToday.set(Calendar.MILLISECOND, 0);

            long diff = calTask.getTimeInMillis() - calToday.getTimeInMillis();
            long millisInDay = (long) HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLIS_IN_SECOND;
            return diff / millisInDay;
            
        } catch (Exception exception)
        {
            return NO_VALID_DATE;
        }
    }

    // בפרויקט הזה משימה נחשבת "דחופה" אם תאריך היעד הוא היום, מחר או מחרתיים
    public static boolean isDueSoon(String date)
    {
        long days = daysLeft(date);

        if (days >= 0 && days <= URGENT_THRESHOLD_DAYS)
        {
            return true;

        }
        return false;
    }

    public static boolean isOverdue(String date) {
        return daysLeft(date) < 0;
    }
}
