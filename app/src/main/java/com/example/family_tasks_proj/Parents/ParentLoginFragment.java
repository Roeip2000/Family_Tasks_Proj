package com.example.family_tasks_proj.Parents;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.family_tasks_proj.Parents_Dashbord_and_mange.ParentDashboardActivity;
import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ׳׳¡׳ ׳”׳×׳—׳‘׳¨׳•׳× ׳׳”׳•׳¨׳” ג€” Fragment ׳©׳׳•׳¦׳’ ׳‘׳×׳•׳ MainActivity.
 *
 * ׳׳—׳¨׳™׳•׳×:
 * - ׳׳‘׳¦׳¢ ׳׳™׳׳•׳× (email + password) ׳“׳¨׳ FirebaseAuth.
 * - ׳‘׳”׳¦׳׳—׳”: ׳₪׳•׳×׳— ParentDashboardActivity ׳•׳¡׳•׳’׳¨ ׳׳× MainActivity.
 * - ׳‘׳›׳™׳©׳׳•׳: ׳׳¦׳™׳’ ׳”׳•׳“׳¢׳× ׳©׳’׳™׳׳”.
 *
 * Layout: fragment_parent_login.xml
 */
public class ParentLoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressLogin;

    public ParentLoginFragment()
    {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_parent_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        progressLogin = view.findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser()
    {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty())
        {
            Toast.makeText(requireContext(), "׳™׳© ׳׳׳׳ ׳׳× ׳›׳ ׳”׳©׳“׳•׳×", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            Toast.makeText(requireContext(), "׳₪׳•׳¨׳׳˜ ׳׳™׳׳™׳™׳ ׳׳ ׳×׳§׳™׳", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task ->
                {
                    if (!isAdded()) return;

                    setLoading(false);
                    if (task.isSuccessful())
                    {
                        startActivity(new Intent(requireActivity(), ParentDashboardActivity.class));
                        requireActivity().finish();
                    }
                    else
                    {
                        String errorMsg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "׳©׳’׳™׳׳× ׳”׳×׳—׳‘׳¨׳•׳× ׳׳ ׳™׳“׳•׳¢׳”";
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** מציג טעינה קצרה ומונע לחיצות כפולות בזמן ההתחברות. */
    private void setLoading(boolean isLoading)
    {
        btnLogin.setEnabled(!isLoading);
        if (progressLogin != null) {
            progressLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}
