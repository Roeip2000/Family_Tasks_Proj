package com.example.family_tasks_proj.utils;

import java.util.Calendar;

// מחלקת עזר לחישוב מצב משימות לפי תאריך יעד
public class DateUtils {

    private static final long MILLIS_IN_DAY = 24L * 60 * 60 * 1000;
    private static final int URGENT_THRESHOLD_DAYS = 2;

    // ערך בטוח לתאריך לא תקין: גדול מ-URGENT_THRESHOLD_DAYS כדי שהמשימה לא תיחשב דחופה/באיחור
    private static final long SAFE_DAYS_LEFT = Long.MAX_VALUE;

    // מחזיר כמה ימים נשארו עד תאריך היעד
    public static long getDaysLeft(String dueDate)
    {
        // בדיקת הגנה: תאריך חסר או לא תקין לא יגרום לקריסה
        if (dueDate == null || dueDate.trim().isEmpty()) {
            return SAFE_DAYS_LEFT;
        }

        int day, month, year;
        try {
            // התאריך נשמר כמחרוזת בפורמט יום/חודש/שנה
            String[] dateParts = dueDate.split("/");
            day = Integer.parseInt(dateParts[0].trim());
            month = Integer.parseInt(dateParts[1].trim());
            year = Integer.parseInt(dateParts[2].trim());
        } catch (Exception exception) {
            // בדיקת הגנה: תאריך חסר או לא תקין לא יגרום לקריסה
            return SAFE_DAYS_LEFT;
        }

        Calendar taskDate = Calendar.getInstance();

        // ב-Calendar חודשים מתחילים מ-0, לכן ממירים חודש רגיל לחודש של Calendar
        int calendarMonth = month - 1;
        taskDate.set(year, calendarMonth, day, 0, 0, 0);
        taskDate.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();

        // מאפסים את השעה כדי להשוות לפי תאריך בלבד
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // מחשבים את ההפרש בין תאריך המשימה לבין היום במילישניות
        long differenceMillis = taskDate.getTimeInMillis() - today.getTimeInMillis();

        // מספר הימים שנשארו עד תאריך היעד
        long daysLeft = differenceMillis / MILLIS_IN_DAY;
        return daysLeft;
    }

    // משימה נחשבת דחופה אם נשארו לה עד יומיים
    public static boolean isDueSoon(String dueDate) {
        long days = getDaysLeft(dueDate);
        return days >= 0 && days <= URGENT_THRESHOLD_DAYS;
    }

    // משימה באיחור אם תאריך היעד כבר עבר
    public static boolean isOverdue(String dueDate) {
        return getDaysLeft(dueDate) < 0;
    }
}
