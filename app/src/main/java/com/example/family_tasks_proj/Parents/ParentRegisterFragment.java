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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.FireBase.FBsingleton;
import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * מסך הרשמה להורה חדש שמוצג בתוך MainActivity.
 * יוצר חשבון ב-FirebaseAuth, שומר את פרטי ההורה ב-Realtime Database,
 * ואז מעביר לדשבורד ההורה.
 */
public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private ProgressBar progressRegister;

    public ParentRegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
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

        btnRegister.setOnClickListener(v -> registerParent());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_DONE || isEnterKey) {
                registerParent();
                return true;
            }
            return false;
        });
    }

    private void registerParent() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(requireContext(), R.string.error_short_password, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task -> {
            if (!isAdded()) return;

            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    FBsingleton.getInstance().setUserData(firstName, lastName, email);
                    // מחכים לשמירת פרופיל ההורה ב-Firebase לפני פתיחת הדשבורד
                    FBsingleton.getInstance().saveParentToFirebase(saveTask -> {
                        if (!isAdded()) return;

                        setLoading(false);
                        if (!saveTask.isSuccessful()) {
                            String saveError = (saveTask.getException() != null)
                                    ? saveTask.getException().getMessage()
                                    : getString(R.string.error_unknown_register);
                            Toast.makeText(requireContext(), saveError, Toast.LENGTH_LONG).show();
                            return;
                        }

                        Toast.makeText(requireContext(), R.string.success_parent_register, Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(
                                requireActivity(),
                                com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity.class
                        ));
                        requireActivity().finish();
                    });
                } else {
                    setLoading(false);
                }
            } else {
                setLoading(false);
                String errorMsg = (task.getException() != null)
                        ? task.getException().getMessage()
                        : getString(R.string.error_unknown_register);
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** מציג טעינה קצרה ומונע לחיצות כפולות בזמן ההרשמה. */
    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        etFirstName.setEnabled(!isLoading);
        etLastName.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? R.string.btn_create_account_loading : R.string.btn_create_account);
        if (progressRegister != null) {
            progressRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
