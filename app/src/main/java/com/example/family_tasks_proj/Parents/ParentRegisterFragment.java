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
import com.google.firebase.auth.FirebaseUser;

// מסך הרשמת הורה חדש ושמירת פרופיל ב-Firebase
public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private ProgressBar progressRegister;

    // חובה לשמור פעולה בונה ריקה כדי שאנדרואיד יוכל ליצור את ה-Fragment מחדש
    public ParentRegisterFragment() {
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

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerParent();
            }
        });

        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                return handlePasswordEditorAction(actionId, event);
            }
        });
    }

    // מאפשר הרשמה גם דרך כפתור Enter במקלדת
    private boolean handlePasswordEditorAction(int actionId, KeyEvent event) {
        boolean isEnterKey = event != null
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                && event.getAction() == KeyEvent.ACTION_DOWN;

        if (actionId == EditorInfo.IME_ACTION_DONE || isEnterKey) {
            registerParent();
            return true;
        }

        return false;
    }

    private void registerParent() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateRegisterFields(firstName, lastName, email, password)) {
            return;
        }

        setLoading(true);

        Task<AuthResult> registerTask = mAuth.createUserWithEmailAndPassword(email, password);
        registerTask.addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                handleRegisterResult(task, firstName, lastName, email);
            }
        });
    }

    private boolean validateRegisterFields(String firstName, String lastName, String email, String password) {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(requireContext(), R.string.error_short_password, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void handleRegisterResult(@NonNull Task<AuthResult> task,
                                      String firstName,
                                      String lastName,
                                      String email) {
        if (!isAdded()) {
            return;
        }

        if (!task.isSuccessful()) {
            setLoading(false);
            showRegisterError(task);
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            setLoading(false);
            return;
        }

        saveParentProfile(firstName, lastName, email);
    }

    // שומר פרופיל הורה ב-Firebase תחת /parents/{uid}
    private void saveParentProfile(String firstName, String lastName, String email) {
        FBsingleton.getInstance().setUserData(firstName, lastName, email);
        FBsingleton.getInstance().saveParentToFirebase(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> saveTask) {
                handleParentProfileSaved(saveTask);
            }
        });
    }

    private void handleParentProfileSaved(@NonNull Task<Void> saveTask) {
        if (!isAdded()) {
            return;
        }

        setLoading(false);
        if (!saveTask.isSuccessful()) {
            showRegisterError(saveTask);
            return;
        }

        Toast.makeText(requireContext(), R.string.success_parent_register, Toast.LENGTH_SHORT).show();
        openParentDashboard();
    }

    private void showRegisterError(@NonNull Task<?> task) {
        String errorMsg;
        if (task.getException() != null) {
            errorMsg = getString(R.string.error_with_details, task.getException().getMessage());
        } else {
            errorMsg = getString(R.string.error_unknown_register);
        }
        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
    }

    private void openParentDashboard() {
        Intent intent = new Intent(requireActivity(), ParentDashboardActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    // מונע לחיצות כפולות בזמן הרשמה
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
