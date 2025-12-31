package com.example.family_tasks_proj.FireBase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    public static FBsingleton getInstance() {
        if (instance == null) {
            instance = new FBsingleton();
        }
        return instance;
    }

    // ===== Setters (לאפליקציה) =====

    public void setUserData(String firstName, String lastName, String email) {
        this.uid = auth.getUid();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // ===== Getters (לאפליקציה) =====

    public String getUid() {
        return uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    // ===== כתיבה לפיירבייס =====

    public void saveParentToFirebase() {
        if (uid == null) return;

        DatabaseReference ref =
                database.getReference("parents").child(uid);

        ParentInFb parent = new ParentInFb(
                uid,
                firstName,
                lastName,
                email
        );

        ref.setValue(parent);
    }
}
