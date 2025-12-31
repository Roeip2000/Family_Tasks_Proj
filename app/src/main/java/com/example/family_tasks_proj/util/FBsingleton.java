package com.example.family_tasks_proj.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FBsingleton {

    private static FBsingleton instance;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private String firstName;
    private String lastName;
    private String email;

    private FBsingleton() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FBsingleton getInstance() {
        if (instance == null) {
            instance = new FBsingleton();
        }
        return instance;
    }

    public void setUserData(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public void saveParentToFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            Map<String, Object> parentData = new HashMap<>();
            parentData.put("firstName", firstName);
            parentData.put("lastName", lastName);
            parentData.put("email", email);

            mDatabase.child("users").child(uid).setValue(parentData);
        }
    }
}
