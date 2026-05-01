package com.example.family_tasks_proj.FireBase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

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
        if (userId == null) {
            return;
        }

        // מעדכן רק את שדות הפרופיל של ההורה בלי לגעת בילדים ובתבניות.
        // הנתיב המרכזי של ההורה הוא parents/{uid}.
        DatabaseReference parentReference = firebaseDatabase.getReference("parents").child(userId);

        // updateChildren מתאים כאן כי הוא מעדכן כמה שדות ביחד ולא מוחק תתי-תיקיות קיימות.
        Map<String, Object> parentData = new HashMap<>();
        parentData.put("uid", userId);
        parentData.put("firstName", parentFirstName);
        parentData.put("lastName", parentLastName);
        parentData.put("email", parentEmail);
        parentData.put("role", "parent");

        parentReference.updateChildren(parentData).addOnCompleteListener(listener);
    }
}
