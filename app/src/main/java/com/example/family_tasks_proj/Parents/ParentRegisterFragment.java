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

import com.example.family_tasks_proj.FireBase.FBsingleton;
import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

/** מסך הרשמת הורה חדש. יוצר משתמש ב-FirebaseAuth ושומר את פרטיו ב-Realtime Database. */
public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressRegister;

    public ParentRegisterFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        progressRegister = view.findViewById(R.id.progressRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { registerParent(); }
        });

        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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
        final String fName = etFirstName.getText().toString().trim();
        final String lName = etLastName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(requireContext(), R.string.error_short_password, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!isAdded()) return;
                if (task.isSuccessful()) {
                    FBsingleton.getInstance().setUserData(fName, lName, email);
                    FBsingleton.getInstance().saveParentToFirebase(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> t) {
                            setLoading(false);
                            if (t.isSuccessful()) {
                                Toast.makeText(requireContext(), R.string.success_parent_register, Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                                requireActivity().finish();
                            }
                        }
                    });
                } else {
                    setLoading(false);
                    Toast.makeText(requireContext(), getString(R.string.error_with_details, (task.getException() != null ? task.getException().getMessage() : getString(R.string.error_unknown_register))), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? R.string.btn_create_account_loading : R.string.btn_create_account);
        if (progressRegister != null) progressRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
