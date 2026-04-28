package com.example.family_tasks_proj.FireBase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

/** מחלקת Singleton לניהול הקשר מול Firebase. מרכזת את פרטי המשתמש והגדרות בסיס. */
public class FBsingleton {

    private static final FBsingleton instance = new FBsingleton();
    private final FirebaseDatabase db;
    private final FirebaseAuth auth;
    private String uid;
    private String firstName;
    private String lastName;
    private String email;

    private FBsingleton() {
        db = FirebaseDatabase.getInstance();
        db.setPersistenceEnabled(true); // מאפשר עבודה ללא אינטרנט
        auth = FirebaseAuth.getInstance();
    }

    public static FBsingleton getInstance() {
        return instance;
    }

    public void setUserData(String first, String last, String email) {
        this.uid = auth.getUid();
        this.firstName = first;
        this.lastName = last;
        this.email = email;
    }

    // שומר את פרטי ההורה ב-Realtime Database
    public void saveParentToFirebase(OnCompleteListener<Void> listener) {
        if (uid == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("email", email);
        data.put("role", "parent");

        if (listener != null) {
            db.getReference("parents").child(uid).updateChildren(data).addOnCompleteListener(listener);
        } else {
            db.getReference("parents").child(uid).updateChildren(data);
        }
    }
}
