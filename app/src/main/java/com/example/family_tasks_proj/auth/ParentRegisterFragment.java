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

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.parent.ParentDashboardActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;

    public ParentRegisterFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerParent();
            }
        });

        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    registerParent();
                    return true;
                }
                return false;
            }
        });
    }

    private void registerParent() {
        final String firstName = etFirstName.getText().toString().trim();
        final String lastName = etLastName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return;
        }

        if (password.length() < 6) {
            return;
        }

        // יצירת משתמש ב-Firebase Auth
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!isAdded()) {
                    return;
                }
                if (task.isSuccessful()) {
                    saveParentToDatabase(firstName, lastName, email);
                }
            }
        });
    }

    // שומר את השם והאימייל של ההורה החדש ב-Realtime Database תחת העץ parents
    private void saveParentToDatabase(String firstName, String lastName, String email) {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        DatabaseReference parentRef = FirebaseDatabase.getInstance().getReference("parents").child(uid);

        parentRef.child("uid").setValue(uid);
        parentRef.child("firstName").setValue(firstName);
        parentRef.child("lastName").setValue(lastName);
        parentRef.child("email").setValue(email);
        parentRef.child("role").setValue("parent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> saveTask) {
                if (!isAdded()) {
                    return;
                }
                if (saveTask.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.success_parent_register, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                    requireActivity().finish();
                }
            }
        });
    }
}
