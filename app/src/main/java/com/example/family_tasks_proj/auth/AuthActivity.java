package com.example.family_tasks_proj.auth;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.family_tasks_proj.R;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        Button btnLoginFragment = findViewById(R.id.btn_login_fragment);

        Button btnRegisterFragment = findViewById(R.id.btn_register_fragment);

        btnLoginFragment.setOnClickListener(v -> loadFragment(new ParentLoginFragment()));
        btnRegisterFragment.setOnClickListener(v -> loadFragment(new ParentRegisterFragment()));

        // Load default fragment
        if (savedInstanceState == null)
        {
            loadFragment(new ParentLoginFragment());
        }
    }

    private void loadFragment(Fragment fragment)
    {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
