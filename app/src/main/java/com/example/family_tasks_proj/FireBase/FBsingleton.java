package com.example.family_tasks_proj.FireBase;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton — גישה מרכזית ל-FirebaseAuth ול-Realtime Database.
 *
 * אחריות:
 * - שומר את פרטי ההורה המחובר בזיכרון (uid, שם, אימייל).
 * - כותב/מעדכן את פרופיל ההורה ב-Firebase בלי למחוק נתוני ילדים קיימים.
 *
 * שימוש: FBsingleton.getInstance()
 *
 * ===== הערות חשובות =====
 * - ה-getInstance() הוא synchronized כדי למנוע יצירת שני instances ב-threads שונים.
 * - saveParentToFirebase() משתמש ב-updateChildren() ולא ב-setValue() —
 *   זה קריטי כדי לא לדרוס את /children ו-/task_templates שכבר קיימים תחת ההורה.
 *
 * ===== הערות לשיפור =====
 * TODO: הוספת callback/listener ל-saveParentToFirebase כדי לדעת אם השמירה הצליחה.
 * TODO: שימוש ב-FirebaseUser.getUid() ישירות במקום שמירה ב-field — מונע מצב
 *       שבו uid לא מסונכרן עם ה-user המחובר.
 */
public class FBsingleton {

    private static FBsingleton instance;

    private final FirebaseDatabase database;
    private final FirebaseAuth auth;

    // פרטי ההורה המחובר — נשמרים בזיכרון
    private String uid;
    private String firstName;
    private String lastName;
    private String email;

    /** constructor פרטי — מונע יצירה ישירה מבחוץ. */
    private FBsingleton() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * מחזיר את ה-instance היחיד; יוצר אותו בפעם הראשונה.
     * synchronized — מונע race condition ב-multi-threading.
     */
    public static synchronized FBsingleton getInstance()
    {
        if (instance == null)
        {
            instance = new FBsingleton();
        }
        return instance;
    }

    /**
     * שומר את פרטי ההורה בזיכרון לשימוש מאוחר.
     * שולף uid אוטומטית מ-FirebaseAuth.
     *
     * @param firstName שם פרטי
     * @param lastName  שם משפחה
     * @param email     אימייל
     */
    public void setUserData(String firstName, String lastName, String email)
    {
        this.uid = auth.getUid();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getUid()       { return uid; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getEmail()     { return email; }

    /**
     * כותב את פרופיל ההורה ל-Firebase בנתיב /parents/{uid}.
     *
     * משתמש ב-updateChildren() כדי לעדכן רק את שדות הפרופיל
     * בלי לדרוס children/ או task_templates/ שכבר קיימים.
     *
     * הערה: אם uid == null (למשל אם setUserData לא נקרא קודם),
     *        המתודה תחזור בשקט בלי לכתוב כלום.
     */
    /**
     * שומר פרופיל הורה ב-Firebase עם callback לדיווח הצלחה/כישלון.
     *
     * @param listener callback — מקבל את תוצאת הכתיבה. null = בלי callback (שקט).
     */
    public void saveParentToFirebase(OnCompleteListener<Void> listener) {
        if (uid == null)
        {
            Log.w("FBsingleton", "saveParentToFirebase: uid == null, לא שומרים");
            return;
        }

        DatabaseReference ref = database.getReference("parents").child(uid);

        // יוצר Map רק עם שדות הפרופיל — לא נוגע בילדים/תבניות
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", uid);
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        profileData.put("email", email);
        profileData.put("role", "parent");

        // updateChildren ולא setValue — שומר על נתונים קיימים!
        if (listener != null) {
            ref.updateChildren(profileData).addOnCompleteListener(listener);
        } else {
            ref.updateChildren(profileData);
        }
    }

    /** overload לתאימות אחורה — ללא callback */
    public void saveParentToFirebase() {
        saveParentToFirebase(null);
    }
}
