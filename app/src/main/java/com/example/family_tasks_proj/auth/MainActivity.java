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

/**
 * מסך כניסה ראשי — Launcher Activity.
 *
 * מציג כפתורים לניווט בין:
 * - התחברות הורה (ParentsLoginFragment)
 * - הרשמת הורה (ParentRegisterFragment)
 * - סריקת QR לילד (ChildQRLoginFragment)
 * - כניסה ישירה לדשבורד ילד (ChildDashboardActivity)
 *
 * כל Fragment נטען לתוך fragmentContainer שב-layout.
 */
public class MainActivity extends AppCompatActivity {

    private Button btnRegister, btnLogin, btnChildQR, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);
        btnChild = findViewById(R.id.btnChild);

        if (savedInstanceState == null)
        {
            openFragment(new ParentsLoginFragment());
        }

        btnRegister.setOnClickListener(v -> openFragment(new ParentRegisterFragment()));
        btnLogin.setOnClickListener(v -> openFragment(new ParentsLoginFragment()));
        btnChildQR.setOnClickListener(v -> openFragment(new ChildQRLoginFragment()));

        // כניסה ישירה ללא QR — משתמש בסשן שמור ב-SharedPreferences (אם קיים)
        btnChild.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, ChildDashboardActivity.class);
            startActivity(intent);
        });
    }

    /** מחליף את ה-Fragment המוצג ב-fragmentContainer ושומר ב-back stack. */
    private void openFragment(Fragment fragment)
    {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).addToBackStack(null).commit();
    }
}