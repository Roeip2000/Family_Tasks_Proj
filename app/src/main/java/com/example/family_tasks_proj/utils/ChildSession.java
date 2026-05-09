package com.example.family_tasks_proj.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * מחלקת עזר לניהול ה-session של הילד ב-SharedPreferences.
 * מרכזת את שמות המפתחות ואת פעולות הקריאה/השמירה/הניקוי במקום אחד,
 * כדי שכל המסכים שצריכים את מזהי ההורה והילד ישתמשו באותו פורמט.
 */
public final class ChildSession {

    public static final String PREFS = "child_session";
    public static final String KEY_PARENT = "parentId";
    public static final String KEY_CHILD = "childId";

    private ChildSession() {
    }

    // מחזיר את מזהה ההורה השמור על המכשיר (או null אם אין session)
    public static String getParentId(Context context) {
        return prefs(context).getString(KEY_PARENT, null);
    }

    // מחזיר את מזהה הילד השמור על המכשיר (או null אם לא נבחר ילד)
    public static String getChildId(Context context) {
        return prefs(context).getString(KEY_CHILD, null);
    }

    // שומר את מזהי ההורה והילד. אם childId ריק, מוחק רק אותו ומשאיר את ההורה.
    public static void save(Context context, String parentId, String childId) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putString(KEY_PARENT, parentId);
        if (childId == null || childId.trim().isEmpty()) {
            editor.remove(KEY_CHILD);
        } else {
            editor.putString(KEY_CHILD, childId);
        }
        editor.apply();
    }

    // מנקה את כל ה-session, משמש בעת התנתקות הילד או כשהילד לא קיים יותר ב-Firebase
    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
