package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.parent.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class ParentLoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_parent_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        
        firebaseAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });
    }

    private void loginUser()
    {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty())
        {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // התחברות ישירה מול Firebase ומעבר לדשבורד במקרה של הצלחה
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(),
                new OnCompleteListener<AuthResult>() {@Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                if (!isAdded())
                {
                    return;
                }
                if (task.isSuccessful())
                {
                    startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                    requireActivity().finish();
                }
                else
                {
                    btnLogin.setEnabled(true);
                    Toast.makeText(requireContext(), "התחברות נכשלה", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}