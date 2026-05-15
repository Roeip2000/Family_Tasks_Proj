package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChildSelectionActivity extends AppCompatActivity {

    private TextView tvNoChildren;
    private Spinner spinnerChildren;
    private Button btnEnter;

    private final List<String> childIds = new ArrayList<>();
    private final List<String> childNames = new ArrayList<>();
    
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);

        spinnerChildren = findViewById(R.id.spinnerChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        btnEnter = findViewById(R.id.btnEnter);

        // קבלת parentId ממסך ה-QR
        parentId = getIntent().getStringExtra("parentId");

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEnterClicked();
            }
        });

        loadChildren();
    }

    // טעינת הילדים מ-Firebase
    private void loadChildren() {
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childIds.clear();
                childNames.clear();
                
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String id = snap.getKey();
                    String firstName = snap.child("firstName").getValue(String.class);
                    String lastName = snap.child("lastName").getValue(String.class);
                    
                    String fullName = "";
                    if (firstName != null) {
                        fullName += firstName.trim();
                    }
                    if (lastName != null) {
                        fullName += " " + lastName.trim();
                    }
                    fullName = fullName.trim();
                    
                    if (fullName.isEmpty()) {
                        fullName = getString(R.string.default_child_name_fallback);
                    }
                    
                    childIds.add(id);
                    childNames.add(fullName);
                }
                
                if (childIds.isEmpty()) {
                    tvNoChildren.setVisibility(View.VISIBLE);
                    spinnerChildren.setVisibility(View.GONE);
                    btnEnter.setEnabled(false);
                    return;
                }
                
                tvNoChildren.setVisibility(View.GONE);
                spinnerChildren.setVisibility(View.VISIBLE);
                btnEnter.setEnabled(true);
                
                // הצגת שמות הילדים ב-Spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ChildSelectionActivity.this, 
                        android.R.layout.simple_spinner_dropdown_item, 
                        childNames
                );
                spinnerChildren.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // מעבר לדשבורד הילד
    private void onEnterClicked() {
        int selectedPosition = spinnerChildren.getSelectedItemPosition();
        
        String childId = childIds.get(selectedPosition);
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra(ChildDashboardActivity.EXTRA_PARENT_ID, parentId);
        intent.putExtra(ChildDashboardActivity.EXTRA_CHILD_ID, childId);
        startActivity(intent);
        finish();
    }
}
