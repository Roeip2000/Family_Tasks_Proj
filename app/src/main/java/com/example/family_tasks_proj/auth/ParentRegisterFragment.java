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

import java.util.HashMap;
import java.util.Map;

/** מסך הרשמת הורה חדש. יוצר משתמש ב-FirebaseAuth ושומר את פרטיו ב-Realtime Database. */
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
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    registerParent();
                    return true;
                }
                return false;
            }
        });
    }

    // מבצע הרשמה ל-Firebase ושומר פרופיל הורה
    private void registerParent() {
        final String firstName = etFirstName.getText().toString().trim();
        final String lastName = etLastName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // בהרשמה שומרים גם שם פרטי ומשפחה, כדי שבהמשך הדשבורד יוכל להציג ברכה אישית.
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(requireContext(), R.string.error_short_password, Toast.LENGTH_SHORT).show();
            return;
        }

        // יוצר משתמש חדש במערכת בעזרת אימייל וסיסמה דרך FirebaseAuth
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!isAdded()) {
                    return;
                }
                if (task.isSuccessful()) {
                    // אם ההרשמה הצליחה, שומרים את שאר פרטי ההורה ב-Realtime Database
                    saveParentToDatabase(firstName, lastName, email);
                } else {
                    String errorMessage;
                    if (task.getException() != null) {
                        errorMessage = task.getException().getMessage();
                    } else {
                        errorMessage = getString(R.string.error_unknown_register);
                    }
                    Toast.makeText(requireContext(), getString(R.string.error_with_details, errorMessage), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveParentToDatabase(String firstName, String lastName, String email) {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        // הפנייה לנתיב של ההורה החדש ב-Realtime Database.
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
                } else {
                    String saveErrorMessage = (saveTask.getException() != null) ? saveTask.getException().getMessage() : getString(R.string.error_unknown_register);
                    Toast.makeText(requireContext(), getString(R.string.error_save_parent_failed, saveErrorMessage), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
