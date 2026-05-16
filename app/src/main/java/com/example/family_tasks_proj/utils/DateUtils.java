package com.example.family_tasks_proj.utils;

import java.util.Calendar;

public class DateUtils {

    private static final int HOURS_IN_DAY = 24;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MILLIS_IN_SECOND = 1000;
    private static final int URGENT_THRESHOLD_DAYS = 2;

    // מחזיר כמה ימים נשארו עד תאריך היעד של המשימה.
    public static long daysLeft(String dateStr) {
        String[] parts = dateStr.split("/");
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
    }

    public static boolean isDueSoon(String date) {
        long days = daysLeft(date);
        return days >= 0 && days <= URGENT_THRESHOLD_DAYS;
    }

    public static boolean isOverdue(String date) {
        return daysLeft(date) < 0;
    }
}
