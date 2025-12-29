package com.example.family_tasks_proj;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class AuthActivity extends AppCompatActivity {

    Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        btnLogin = findViewById(R.id.btnLoginFrag);
        btnRegister = findViewById(R.id.btnRegisterFrag);

        // ברירת מחדל – Login
        openFragment(new ParentsLoginFragment());

        btnLogin.setOnClickListener(v ->
                openFragment(new ParentRegisterFragment())
        );

        btnRegister.setOnClickListener(v ->
                openFragment(new ParentRegisterFragment())
        );
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.authFragmentContainer, fragment)
                .commit();
    }
}
