package com.example.family_tasks_proj;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ParentsRegisterActivity extends AppCompatActivity {

    // משתנה שאחראי על Firebase Authentication
    private FirebaseAuth mAuth;

    // רכיבי המסך
    EditText etEmail, etPassword;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parents_register);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // חיבור בין הקוד לרכיבים ב-XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        // מאזין ללחיצה על כפתור ההרשמה
        btnRegister.setOnClickListener(v -> registerParent());
    }

    /**
     * פונקציה שאחראית על הרשמת ההורה
     */
    private void registerParent() {

        // שליפת הטקסט מהשדות
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        // בדיקה בסיסית – שלא יהיו שדות ריקים
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת משתמש ב-Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        // ההרשמה הצליחה
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();

                        // בהמשך: מעבר למסך התחברות / Dashboard
                    } else {
                        // שגיאה בהרשמה
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
