package com.example.family_tasks_proj;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

     Button btnRegister, btnLogin, btnChildQR;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);

        // מעבר להרשמה להורים
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ParentsRegisterActivity.class);
            startActivity(intent);
        });

        // מעבר להתחברות הורים
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ParentsLoginActivity.class);
            startActivity(intent);
        });

        // מעבר לכניסת ילד עם QR
        btnChildQR.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChildQRLoginActivity.class);
            startActivity(intent);
        });




    }
}