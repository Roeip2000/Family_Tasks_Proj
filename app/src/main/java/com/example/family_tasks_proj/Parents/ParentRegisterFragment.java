package com.example.family_tasks_proj.Parents;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.FireBase.FBsingleton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * מסך הרשמה להורה חדש — Fragment שמוצג בתוך MainActivity.
 *
 * אחריות:
 * - יוצר חשבון חדש ב-FirebaseAuth (email + password).
 * - שומר את פרופיל ההורה ב-Realtime Database דרך FBsingleton.
 * - מעביר ל-ParentDashboardActivity בהצלחה.
 *
 * Layout: fragment_parent_register.xml
 *
 * ===== באגים שתוקנו =====
 * BUG-FIX: task.getException() יכול להחזיר null — נוסף null-check.
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף ולידציה לאורך סיסמה (מינימום 6 תווים — דרישת Firebase).
 * TODO: להוסיף ProgressBar בזמן ההרשמה.
 * TODO: להוסיף ולידציה לפורמט email.
 */
public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;

    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;

    /** constructor ריק — חובה ל-Fragment. */
    public ParentRegisterFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_parent_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // חיבור שדות מה-layout
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName  = view.findViewById(R.id.etLastName);
        etEmail     = view.findViewById(R.id.etEmail);
        etPassword  = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerParent());
    }

    /**
     * יוצר חשבון חדש ב-FirebaseAuth ושומר פרופיל ב-DB.
     *
     * תהליך:
     * 1. ולידציה — בודק ששדות לא ריקים.
     * 2. createUserWithEmailAndPassword — יוצר חשבון ב-Firebase.
     * 3. FBsingleton.setUserData + saveParentToFirebase — שומר פרופיל ב-Realtime Database.
     * 4. ניווט ל-ParentDashboardActivity.
     *
     * הערה חשובה: FBsingleton.saveParentToFirebase() משתמש ב-updateChildren()
     *              ולא ב-setValue(), כדי לא לדרוס ילדים/תבניות שכבר קיימים.
     */
    private void registerParent()
    {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task ->
        {
                    if (task.isSuccessful())
                    {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null)
                        {
                            // שמירת פרטי ההורה ב-Singleton ואז כתיבה ל-Firebase
                            FBsingleton.getInstance().setUserData(firstName, lastName, email);
                            FBsingleton.getInstance().saveParentToFirebase();

                            Toast.makeText(requireContext(), "ההרשמה הצליחה!", Toast.LENGTH_SHORT).show();

                            // מעבר לדשבורד — סוגר MainActivity כדי שלא יחזור אליה
                            startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                            requireActivity().finish();
                        }
                    }
                    else
                    {
                        // BUG-FIX: null-check ל-getException כדי למנוע NPE
                        String errorMsg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "שגיאה לא ידועה";
                        Toast.makeText(requireContext(), "שגיאה: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
