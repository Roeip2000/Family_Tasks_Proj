package com.example.family_tasks_proj.parent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.Child;
import com.example.family_tasks_proj.child.GenerateQRActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ManageChildrenActivity extends AppCompatActivity {

    EditText etFirstName, etLastName;
    Button btnAddChild;

    DatabaseReference db;
    String parentUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnAddChild = findViewById(R.id.btnAddChild);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            parentUID = currentUser.getUid();
        } else {
            Toast.makeText(this, "You must be logged in to add a child.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db = FirebaseDatabase.getInstance().getReference(); // Get root reference

        btnAddChild.setOnClickListener(v -> addChild());
    }

    private void addChild() {
        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = db.child("users").child(parentUID).child("children").push().getKey();

        if (childId != null) {
            Child newChild = new Child(first, last);
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/users/" + parentUID + "/children/" + childId, newChild);
            childUpdates.put("/child_to_parent/" + childId, parentUID);

            db.updateChildren(childUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent i = new Intent(this, GenerateQRActivity.class);
                    i.putExtra("childId", childId);
                    startActivity(i);
                } else {
                    Toast.makeText(ManageChildrenActivity.this, "Failed to add child.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
