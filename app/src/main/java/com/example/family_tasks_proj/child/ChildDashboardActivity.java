package com.example.family_tasks_proj.child;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChildDashboardActivity extends AppCompatActivity {

    TextView tvChildName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        tvChildName = findViewById(R.id.tvChildName);

        String childId = getIntent().getStringExtra("childId");
        String parentId = getIntent().getStringExtra("parentId");

        if (childId != null && parentId != null) {
            DatabaseReference childRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(parentId).child("children").child(childId);

            childRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Child child = snapshot.getValue(Child.class);
                    if (child != null) {
                        tvChildName.setText("Welcome, " + child.firstName + " " + child.lastName);
                    } else {
                        Toast.makeText(ChildDashboardActivity.this, "Child data not found.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChildDashboardActivity.this, "Database error.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Child ID or Parent ID not found.", Toast.LENGTH_SHORT).show();
        }
    }
}
