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

/**
 * מסך התחברות הורה בתוך MainActivity.
 * בודק אימייל וסיסמה, מתחבר דרך FirebaseAuth,
 * ובכניסה מוצלחת פותח את ParentDashboardActivity.
 */
public class ParentLoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private ProgressBar progressLogin;

    // constructor ריק חובה ל-Fragment כדי שאנדרואיד יוכל ליצור אותו מחדש
    public ParentLoginFragment() {
    }

    // יוצר את ה-layout של מסך התחברות ההורה
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_login, container, false);
    }

    // מחבר views ומגדיר פעולות לחיצה והקלדה
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

    // מאפשר להתחבר גם בלחיצה על Done במקלדת
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

    // בודק שדות התחברות ואז שולח את האימייל והסיסמה ל-FirebaseAuth
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateLoginFields(email, password)) {
            return;
        }

        setLoading(true);

        // קריאה ל-FirebaseAuth: אימות ההורה לפי אימייל וסיסמה
        Task<AuthResult> loginTask = mAuth.signInWithEmailAndPassword(email, password);
        loginTask.addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                handleLoginResult(task);
            }
        });
    }

    // בודק שדות ריקים ואימייל לא תקין לפני פנייה ל-Firebase
    private boolean validateLoginFields(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // מטפל בתוצאה שחזרה מ-FirebaseAuth אחרי ניסיון התחברות
    private void handleLoginResult(@NonNull Task<AuthResult> task) {
        if (!isAdded()) {
            return;
        }

        setLoading(false);
        if (task.isSuccessful()) {
            openParentDashboard();
            return;
        }

        String errorMsg;
        if (task.getException() != null) {
            errorMsg = task.getException().getMessage();
        } else {
            errorMsg = getString(R.string.error_unknown_login);
        }
        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
    }

    // פותח את דשבורד ההורה רק אחרי התחברות מוצלחת
    private void openParentDashboard() {
        Intent intent = new Intent(requireActivity(), ParentDashboardActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    // מציג טעינה קצרה ומונע לחיצות כפולות בזמן התחברות
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
