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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.FireBase.FBsingleton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * ׳׳¡׳ ׳”׳¨׳©׳׳” ׳׳”׳•׳¨׳” ׳—׳“׳© ג€” Fragment ׳©׳׳•׳¦׳’ ׳‘׳×׳•׳ MainActivity.
 *
 * ׳׳—׳¨׳™׳•׳×:
 * - ׳™׳•׳¦׳¨ ׳—׳©׳‘׳•׳ ׳—׳“׳© ׳‘-FirebaseAuth (email + password).
 * - ׳©׳•׳׳¨ ׳׳× ׳₪׳¨׳•׳₪׳™׳ ׳”׳”׳•׳¨׳” ׳‘-Realtime Database ׳“׳¨׳ FBsingleton.
 * - ׳׳¢׳‘׳™׳¨ ׳-ParentDashboardActivity ׳‘׳”׳¦׳׳—׳”.
 *
 * Layout: fragment_parent_register.xml
 */
public class ParentRegisterFragment extends Fragment {

    private FirebaseAuth mAuth;

    private EditText etFirstName, etLastName, etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressRegister;

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

        mAuth = FirebaseAuth.getInstance();

        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName  = view.findViewById(R.id.etLastName);
        etEmail     = view.findViewById(R.id.etEmail);
        etPassword  = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        progressRegister = view.findViewById(R.id.progressRegister);

        btnRegister.setOnClickListener(v -> registerParent());
    }

    private void registerParent()
    {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "׳™׳© ׳׳׳׳ ׳׳× ׳›׳ ׳”׳©׳“׳•׳×", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "׳₪׳•׳¨׳׳˜ ׳׳™׳׳™׳™׳ ׳׳ ׳×׳§׳™׳", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(requireContext(), "׳”׳¡׳™׳¡׳׳” ׳—׳™׳™׳‘׳× ׳׳”׳›׳™׳ ׳׳₪׳—׳•׳× 6 ׳×׳•׳•׳™׳", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task ->
        {
                    if (!isAdded()) return;

                    setLoading(false);
                    if (task.isSuccessful())
                    {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null)
                        {
                            FBsingleton.getInstance().setUserData(firstName, lastName, email);
                            FBsingleton.getInstance().saveParentToFirebase();

                            Toast.makeText(requireContext(), "׳”׳”׳¨׳©׳׳” ׳”׳¦׳׳™׳—׳”!", Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                            requireActivity().finish();
                        }
                    }
                    else
                    {
                        String errorMsg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "׳©׳’׳™׳׳” ׳׳ ׳™׳“׳•׳¢׳”";
                        Toast.makeText(requireContext(), "׳©׳’׳™׳׳”: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** מציג טעינה קצרה ומונע לחיצות כפולות בזמן ההרשמה. */
    private void setLoading(boolean isLoading)
    {
        btnRegister.setEnabled(!isLoading);
        if (progressRegister != null) {
            progressRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
