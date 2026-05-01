package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

/** מסך התחברות הורה למערכת באמצעות אימייל וסיסמה. */
public class ParentLoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    public ParentLoginFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });

        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginUser();
                    return true;
                }
                return false;
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // בדיקת קלט בסיסית לפני פנייה ל-Firebase, כדי לתת למשתמש תשובה מידית.
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        // מבצע כניסה למערכת בעזרת אימייל וסיסמה דרך FirebaseAuth
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!isAdded()) {
                    return;
                }
                if (task.isSuccessful()) {
                    // אם הכניסה הצליחה, עוברים לדשבורד של ההורה
                    // אין צורך להעביר uid ב-Intent כי FirebaseAuth יודע מי המשתמש המחובר.
                    startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                    requireActivity().finish();
                } else {
                    String errorMessage;
                    if (task.getException() != null) {
                        errorMessage = task.getException().getMessage();
                    } else {
                        errorMessage = getString(R.string.error_try_again);
                    }
                    Toast.makeText(requireContext(), getString(R.string.error_login_failed, errorMessage), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
