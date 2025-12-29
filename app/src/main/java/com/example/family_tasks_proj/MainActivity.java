package com.example.family_tasks_proj;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    Button btnRegister, btnLogin, btnChildQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);

        // הרשמה → Fragment
        btnRegister.setOnClickListener(v ->
                openFragment(new ParentRegisterFragment())
        );

        // התחברות → Fragment
        btnLogin.setOnClickListener(v ->
                openFragment(new ParentsLoginFragment())
        );

        // QR ילד (Activity רגיל)
        btnChildQR.setOnClickListener(v ->
                startActivity(
                        new android.content.Intent(
                                this,
                                ChildQRLoginActivity.class
                        )
                )
        );
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
