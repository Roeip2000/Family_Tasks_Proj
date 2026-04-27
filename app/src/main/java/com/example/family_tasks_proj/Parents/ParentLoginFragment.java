package com.example.family_tasks_proj.Parents;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

// מסך התחברות הורה דרך FirebaseAuth
public class ParentLoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private ProgressBar progressLogin;

    // חובה לשמור פעולה בונה ריקה כדי שאנדרואיד יוכל ליצור את ה-Fragment מחדש
    public ParentLoginFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        progressLogin = view.findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });

        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                return handlePasswordEditorAction(actionId, event);
            }
        });
    }

    // מאפשר התחברות גם דרך כפתור Enter במקלדת
    private boolean handlePasswordEditorAction(int actionId, KeyEvent event) {
        boolean isEnterKey = event != null
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_DOWN;

        if (actionId == EditorInfo.IME_ACTION_DONE || isEnterKey) {
            loginUser();
            return true;
        }

        return false;
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!isAdded()) return;
                setLoading(false);
                if (task.isSuccessful()) {
                    startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                    requireActivity().finish();
                } else {
                    String errorMsg = task.getException() != null ? getString(R.string.error_with_details, task.getException().getMessage()) : getString(R.string.error_unknown_login);
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // מונע לחיצות כפולות בזמן התחברות
    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? R.string.btn_login_loading : R.string.btn_login);
        if (progressLogin != null) {
            progressLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
