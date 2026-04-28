package com.example.family_tasks_proj.FireBase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// מחלקה שעוזרת לנו לגשת ל-Firebase מכל מקום באפליקציה בקלות
public class FBsingleton {
    
    private static final FBsingleton instance = new FBsingleton();

    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase firebaseDatabase;

    private String parentFirstName, parentLastName, parentEmail;
    private String userId;

    private FBsingleton() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
    }

    public static FBsingleton getInstance() {
        return instance;
    }

    // שמירת הפרטים שמילאו בטופס ההרשמה
    public void setUserData(String firstName, String lastName, String email) {
        this.parentFirstName = firstName;
        this.parentLastName = lastName;
        this.parentEmail = email;
        this.userId = firebaseAuth.getUid();
    }

    // שומר את ההורה ב-Database אחרי שההרשמה הצליחה
    public void saveParentToFirebase(OnCompleteListener<Void> listener) {
        if (userId == null) {
            userId = firebaseAuth.getUid();
        }
        if (userId == null) return;

        // שמירה ישירה של כל שדה כדי להימנע משימוש במבני נתונים מורכבים כמו HashMap
        DatabaseReference parentReference = firebaseDatabase.getReference("parents").child(userId);
        
        parentReference.child("uid").setValue(userId);
        parentReference.child("firstName").setValue(parentFirstName);
        parentReference.child("lastName").setValue(parentLastName);
        parentReference.child("email").setValue(parentEmail);
        parentReference.child("role").setValue("parent").addOnCompleteListener(listener);
    }
}
