package com.example.family_tasks_proj.FireBase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FBsingleton {

    private static FBsingleton instance;

    private final FirebaseDatabase database;
    private final FirebaseAuth auth;

    // נתונים זמניים לאפליקציה
    private String uid;
    private String firstName;
    private String lastName;
    private String email;

    private FBsingleton() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static FBsingleton getInstance()
    {
        if (instance == null)
        {
            instance = new FBsingleton();
        }
        return instance;
    }

    // ===== Setters (לאפליקציה) =====

    public void setUserData(String firstName, String lastName, String email)
    {
        this.uid = auth.getUid();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // ===== Getters (לאפליקציה) =====

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

    // ===== כתיבה לפיירבייס =====

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
