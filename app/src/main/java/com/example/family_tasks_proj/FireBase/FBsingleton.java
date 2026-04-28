package com.example.family_tasks_proj.FireBase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

// Singleton = מחלקה שיש ממנה רק מופע אחד בכל האפליקציה
public class FBsingleton {

    private static final FBsingleton instance = new FBsingleton();

    private final FirebaseDatabase database;
    private final FirebaseAuth auth;

    private String uid;
    private String firstName;
    private String lastName;
    private String email;

    private FBsingleton() {
        database = FirebaseDatabase.getInstance();
        // setPersistenceEnabled = שומר נתונים גם אם אין אינטרנט
        database.setPersistenceEnabled(true);
        auth = FirebaseAuth.getInstance();
    }

    public static FBsingleton getInstance() {
        return instance;
    }

    public void setUserData(String firstName, String lastName, String email) {
        this.uid = auth.getUid();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public void saveParentToFirebase(OnCompleteListener<Void> listener) {
        if (uid == null) return;

        DatabaseReference ref = database.getReference("parents").child(uid);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", uid);
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        profileData.put("email", email);
        profileData.put("role", "parent");

        // updateChildren ולא setValue — שומר על ילדים ותבניות שכבר קיימים
        if (listener != null) {
            Task<Void> updateTask = ref.updateChildren(profileData);
            updateTask.addOnCompleteListener(listener);
        } else {
            ref.updateChildren(profileData);
        }
    }
}
