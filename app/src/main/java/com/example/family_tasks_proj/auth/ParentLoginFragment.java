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

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.parent.ParentDashboardActivity;
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
        // יצירת התצוגה של Fragment ההתחברות מתוך קובץ ה-XML
        return inflater.inflate(R.layout.fragment_parent_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // חיבור רכיבי המסך מה-XML לקוד
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        // קבלת אובייקט FirebaseAuth לצורך התחברות ההורה
        firebaseAuth = FirebaseAuth.getInstance();

        // לחיצה על הכפתור מפעילה ניסיון התחברות
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });
    }

    private void loginUser()
    {
        // קבלת האימייל והסיסמה מהשדות
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // מניעת לחיצה כפולה בזמן שההתחברות מתבצעת
        btnLogin.setEnabled(false);

        // התחברות מול Firebase בעזרת אימייל וסיסמה
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(),
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            // במקרה של הצלחה עוברים לדשבורד ההורה וסוגרים את מסך הכניסה
                            startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                            requireActivity().finish();
                        }
                        else
                        {
                            // במקרה של כישלון מחזירים את הכפתור לפעיל ומציגים הודעה
                            btnLogin.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.error_login_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
