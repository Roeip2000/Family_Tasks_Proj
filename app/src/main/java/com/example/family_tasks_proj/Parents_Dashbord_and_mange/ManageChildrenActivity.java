package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.Child_Login.Child;
import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName;
    private Button btnAddChild;

    private DatabaseReference db;
    private String parentUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        btnAddChild = findViewById(R.id.btnAddChild);

        db = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        parentUID = currentUser.getUid();

        btnAddChild.setOnClickListener(v -> addChild());
    }

    private void addChild() {
        String first = etFirstName.getText().toString().trim();
        String last  = etLastName.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ childId תחת parents/<parentUID>/children
        String childId = db.child("parents").child(parentUID).child("children").push().getKey();
        if (childId == null) {
            Toast.makeText(this, "Failed to create childId", Toast.LENGTH_SHORT).show();
            return;
        }

        Child newChild = new Child(first, last);

        btnAddChild.setEnabled(false);

        // ✅ שומרים רק תחת ההורה (אין child_to_parent)
        db.child("parents")
                .child(parentUID)
                .child("children")
                .child(childId)
                .setValue(newChild)
                .addOnCompleteListener(task -> {
                    btnAddChild.setEnabled(true);

                    if (task.isSuccessful()) {
                        Intent i = new Intent(ManageChildrenActivity.this, GenerateQRActivity.class);
                        i.putExtra("parentId", parentUID);
                        i.putExtra("childId", childId);
                        startActivity(i);
                    } else {
                        Toast.makeText(this,
                                "Failed: " + (task.getException() != null ? task.getException().getMessage() : "unknown"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
