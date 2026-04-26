package com.example.family_tasks_proj.FireBase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/** מחלקת יחיד לגישה ל-Firebase ולשמירת פרטי ההורה. */
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
        database.setPersistenceEnabled(true);
        auth = FirebaseAuth.getInstance();
    }

    /** מחזיר את המופע היחיד של המחלקה. */
    public static synchronized FBsingleton getInstance() {
        if (instance == null) {
            instance = new FBsingleton();
        }
        return instance;
    }

    // שומר פרטי הורה בזיכרון ושולף uid מ-FirebaseAuth
    public void setUserData(String firstName, String lastName, String email) {
        this.uid = auth.getUid();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getUid()       { return uid; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getEmail()     { return email; }

    // שומר פרופיל הורה ב-Firebase עם מאזין תוצאה אופציונלי
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
