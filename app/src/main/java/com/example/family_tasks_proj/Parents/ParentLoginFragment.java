package com.example.family_tasks_proj.Parents;

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

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * מסך התחברות להורה — Fragment שמוצג בתוך MainActivity.
 *
 * אחריות:
 * - מבצע אימות (email + password) דרך FirebaseAuth.
 * - בהצלחה: פותח ParentDashboardActivity וסוגר את MainActivity.
 * - בכישלון: מציג הודעת שגיאה.
 *
 * Layout: fragment_parent_login.xml
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף ולידציה לפורמט אימייל (Patterns.EMAIL_ADDRESS).
 * TODO: להוסיף ProgressBar/אינדיקטור טעינה בזמן ההתחברות.
 * TODO: להציג הודעת שגיאה מפורטת מ-Firebase (task.getException().getMessage())
 *       במקום הודעה גנרית "Authentication failed".
 */
public class ParentLoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;

    /** constructor ריק — חובה ל-Fragment (מערכת Android יוצרת מחדש בסיבוב מסך). */
    public ParentLoginFragment()
    {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_parent_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // חיבור שדות מה-layout
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> loginUser());
    }

    /**
     * מבצע התחברות עם email + password דרך FirebaseAuth.
     *
     * בהצלחה — פותח ParentDashboardActivity וסוגר את ה-Activity הנוכחי.
     * בכישלון — מציג Toast עם הודעת שגיאה.
     *
     * הערה: requireActivity() ב-addOnCompleteListener מבטיח שה-callback
     *        ירוץ רק אם ה-Activity עדיין חי — מונע crash בסיבוב מסך.
     */
    private void loginUser()
    {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // ולידציה בסיסית — שדות ריקים
        if (email.isEmpty() || password.isEmpty())
        {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task ->
                {
                    if (task.isSuccessful())
                    {
                        // התחברות הצליחה — מעבר לדשבורד הורה
                        startActivity(new Intent(getActivity(), ParentDashboardActivity.class));
                        requireActivity().finish(); // סוגר את MainActivity כדי שלא יחזור אליה
                    }
                    else
                    {
                        // הערה: כדאי להציג task.getException().getMessage() לפירוט
                        Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
