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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // קבלת אובייקט FirebaseAuth לצורך הרשמת הורה
        firebaseAuth = FirebaseAuth.getInstance();

        // חיבור רכיבי המסך מה-XML לקוד
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
    }

    private void registerParent() {
        // קבלת הנתונים שההורה כתב בטופס
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // בדיקה שלא נשלחים ל-Firebase שדות ריקים
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה בסיסית לאורך הסיסמה לפני יצירת המשתמש
        if (password.length() < 6) {
            Toast.makeText(requireContext(), R.string.error_short_password, Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        // יצירת חשבון התחברות ב-FirebaseAuth
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(),
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //  אחרי יצירת המשתמש שומרים את פרטי ההורה במסד הנתונים
                            saveParentToDatabase(firstName, lastName, email);
                        } else {
                            btnRegister.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.register_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveParentToDatabase(String firstName, String lastName, String email)
    {
        // uid הוא המזהה הייחודי ש-FirebaseAuth נתן להורה החדש
        String uid = firebaseAuth.getUid();

        // שמירת פרטי ההורה תחת parents/{uid}
        DatabaseReference parentRef = FirebaseDatabase.getInstance().getReference("parents").child(uid);

        // אריזת פרטי ההורה לשמירה אחת ב-Firebase
        HashMap<String, Object> parentData = new HashMap<>();
        parentData.put("uid", uid);
        parentData.put("firstName", firstName);
        parentData.put("lastName", lastName);
        parentData.put("email", email);
        parentData.put("role", "parent");

        parentRef.setValue(parentData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> saveTask) {
                if (saveTask.isSuccessful()) {
                    // אם השמירה הצליחה עוברים לדשבורד ההורה
                    startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                    requireActivity().finish();
                } else {
                    btnRegister.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.save_parent_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
