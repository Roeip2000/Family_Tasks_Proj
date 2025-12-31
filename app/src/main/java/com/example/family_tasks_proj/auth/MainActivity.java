package com.example.family_tasks_proj.auth;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.Child_Login.ChildQRLoginFragment;
import com.example.family_tasks_proj.Login_Register_Parents.ParentRegisterFragment;
import com.example.family_tasks_proj.Login_Register_Parents.ParentsLoginFragment;
import com.example.family_tasks_proj.R;

public class MainActivity extends AppCompatActivity {

    private Button btnRegister, btnLogin, btnChildQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        btnChildQR = findViewById(R.id.btnChildQR);

        // Fragment ברירת מחדל (כדי לוודא שהקונטיינר עובד)
        if (savedInstanceState == null) {
            openFragment(new ParentsLoginFragment());
        }

        // הרשמה → Fragment
        btnRegister.setOnClickListener(v ->
                openFragment(new ParentRegisterFragment())
        );

        // התחברות → Fragment
        btnLogin.setOnClickListener(v ->
                openFragment(new ParentsLoginFragment())
        );

        // QR ילד → Fragment
        btnChildQR.setOnClickListener(v ->
                openFragment(new ChildQRLoginFragment())
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
