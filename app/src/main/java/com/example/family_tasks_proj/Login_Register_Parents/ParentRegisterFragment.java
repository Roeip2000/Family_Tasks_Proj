package com.example.family_tasks_proj.Login_Register_Parents;

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

import com.example.family_tasks_proj.FireBase.FBsingleton;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * מסך הרשמה להורה חדש.
 *
 * אחריות:
 * - יוצר חשבון ב-FirebaseAuth (email + password).
 * - שומר את פרופיל ההורה ב-Realtime Database דרך FBsingleton.
 * - מעביר ל-ParentDashboardActivity בהצלחה.
 */
public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;

    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;

    public ParentRegisterFragment()
    {
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

        mAuth = FirebaseAuth.getInstance();

        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName  = view.findViewById(R.id.etLastName);
        etEmail     = view.findViewById(R.id.etEmail);
        etPassword  = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerParent());
    }

    /**
     * יוצר חשבון חדש ב-FirebaseAuth, שומר פרופיל ב-DB, ועובר לדשבורד.
     *
     * Side-effects:
     * - כותב ל-/parents/{uid} ב-Realtime Database.
     * - סוגר את ה-Activity הנוכחי (מסך הכניסה).
     */
    private void registerParent()
    {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty())
        {

            Toast.makeText(getContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();

            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task ->
                {

                    if (task.isSuccessful())
                    {

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null)
                        {
                            Toast.makeText(getContext(), "שגיאה: לא ניתן לאמת משתמש", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String uid = user.getUid();

                        // שמירה ב־Singleton
                        FBsingleton.getInstance().setUserData(firstName, lastName, email);

                        FBsingleton.getInstance().saveParentToFirebase();

                        Toast.makeText(getContext(), "הרשמה בוצעה בהצלחה", Toast.LENGTH_SHORT).show();

                        // מעבר לדשבורד
                        startActivity(new Intent(getActivity(), ParentDashboardActivity.class));

                        requireActivity().finish();

                    }
                    else
                    {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(getContext(), "שגיאה: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
