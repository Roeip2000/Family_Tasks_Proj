package com.example.family_tasks_proj;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;

    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;

    public ParentRegisterFragment() {
        // חובה קונסטרקטור ריק
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_parent_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // חיבור ל־XML
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName  = view.findViewById(R.id.etLastName);
        etEmail     = view.findViewById(R.id.etEmail);
        etPassword  = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerParent());
    }

    private void registerParent() {

        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || password.isEmpty()) {

            Toast.makeText(getContext(),
                    "נא למלא את כל השדות",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        String uid = user.getUid();

                        // שמירה ב־Singleton
                        FBsingleton.getInstance().setUserData(
                                firstName,
                                lastName,
                                email
                        );
                        FBsingleton.getInstance().saveParentToFirebase();

                        Toast.makeText(getContext(),
                                "הרשמה בוצעה בהצלחה",
                                Toast.LENGTH_SHORT).show();

                        // מעבר לדשבורד
                        startActivity(new Intent(
                                getActivity(),
                                ParentDashboardActivity.class
                        ));

                        requireActivity().finish();

                    } else {
                        Toast.makeText(getContext(),
                                "שגיאה: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
