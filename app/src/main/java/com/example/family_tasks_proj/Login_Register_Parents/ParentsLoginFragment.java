package com.example.family_tasks_proj.Login_Register_Parents;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;

public class ParentsLoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;

    public ParentsLoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mAuth = FirebaseAuth.getInstance();

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        startActivity(new Intent(
                                requireActivity(),
                                ParentDashboardActivity.class
                        ));
                        requireActivity().finish();

                    } else {
                        Toast.makeText(getContext(),
                                "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
