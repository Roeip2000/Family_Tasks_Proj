package com.example.family_tasks_proj.FireBase;

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
 */
public class FBsingleton {

    private static FBsingleton instance;

    private final FirebaseDatabase database;
    private final FirebaseAuth auth;

    private String uid;
    private String firstName;
    private String lastName;
    private String email;

    private FBsingleton() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /** מחזיר את ה-instance היחיד; יוצר אותו בפעם הראשונה. */
    public static FBsingleton getInstance()
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

    public String getUid()
    {
        return uid;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public String getEmail()
    {
        return email;
    }

    /**
     * כותב את פרופיל ההורה ל-Firebase בנתיב /parents/{uid}.
     *
     * משתמש ב-updateChildren כדי לעדכן רק את שדות הפרופיל
     * בלי לדרוס children/ או task_templates/ שכבר קיימים.
     */
    public void saveParentToFirebase() {
        if (uid == null)
        {
            return;
        }

        DatabaseReference ref = database.getReference("parents").child(uid);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", uid);
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        profileData.put("email", email);
        profileData.put("role", "parent");

        ref.updateChildren(profileData);
    }
}
