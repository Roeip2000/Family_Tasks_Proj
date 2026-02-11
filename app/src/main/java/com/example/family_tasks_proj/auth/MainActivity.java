package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


import com.example.family_tasks_proj.Child_Login.ChildQRLoginFragment;
import com.example.family_tasks_proj.Login_Register_Parents.ParentRegisterFragment;
import com.example.family_tasks_proj.Login_Register_Parents.ParentsLoginFragment;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnRegister, btnLogin, btnChildQR, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // קישור כפתורים מה-XML
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);
        btnChild = findViewById(R.id.btnChild); // תיקון: השם תואם ל-ID ב-XML

        // Fragment ברירת מחדל
        if (savedInstanceState == null)
        {
            openFragment(new ParentsLoginFragment());
        }

        // לוגיקת כפתורים למעבר בין פרגמנטים
        btnRegister.setOnClickListener(v -> openFragment(new ParentRegisterFragment()));
        btnLogin.setOnClickListener(v -> openFragment(new ParentsLoginFragment()));
        btnChildQR.setOnClickListener(v -> openFragment(new ChildQRLoginFragment()));

        // מעבר ישיר ל-Activity של הילד באמצעות Intent
        btnChild.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, ChildDashboardActivity.class);
            startActivity(intent);
        });
    }

    private void openFragment(Fragment fragment)
    {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).addToBackStack(null).commit();
    }
}