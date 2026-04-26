package com.example.family_tasks_proj.FireBase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * מחלקת יחיד לגישה מרכזית ל-FirebaseAuth ול-Realtime Database.
 *
 * אחריות:
 * - שומר את פרטי ההורה המחובר בזיכרון (uid, שם, אימייל).
 * - כותב/מעדכן את פרופיל ההורה ב-Firebase בלי למחוק נתוני ילדים קיימים.
 *
 * ===== הערות חשובות =====
 * - ה-getInstance() נעול כדי למנוע יצירת שני מופעים במקביל.
 * - saveParentToFirebase() משתמש ב-updateChildren() ולא ב-setValue() —
 *   זה קריטי כדי לא לדרוס את /children ו-/task_templates שכבר קיימים תחת ההורה.
 * - saveParentToFirebase(listener) מאפשר לקבל תשובה אחרי הכתיבה — שימושי בבדיקות הרשמה.
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

    /** פעולה בונה פרטית — מונעת יצירה ישירה מבחוץ. */
    private FBsingleton() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * מחזיר את המופע היחיד של המחלקה; יוצר אותו בפעם הראשונה.
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
     * שומר פרופיל הורה ב-Firebase עם מאזין שמחזיר הצלחה או כישלון.
     *
     * @param listener מאזין שמקבל את תוצאת הכתיבה. null אומר שלא צריך מאזין.
     */
    public void saveParentToFirebase(OnCompleteListener<Void> listener) {
        if (uid == null) return;

        DatabaseReference ref = database.getReference("parents").child(uid);

        // יוצר מפת נתונים רק עם שדות הפרופיל — לא נוגע בילדים/תבניות
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", uid);
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        profileData.put("email", email);
        profileData.put("role", "parent");

        // updateChildren ולא setValue — שומר על נתונים קיימים!
        if (listener != null) {
            Task<Void> updateTask = ref.updateChildren(profileData);
            updateTask.addOnCompleteListener(listener);
        } else {
            ref.updateChildren(profileData);
        }
    }

    /** גרסה נוספת לתאימות אחורה — ללא מאזין תוצאה. */
    public void saveParentToFirebase() {
        saveParentToFirebase(null);
    }
}
