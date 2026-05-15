package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

    public static final String EXTRA_PARENT_ID = "parentId";

    private TextView tvNoChildren;
    private Spinner spinnerChildren;
    private Button btnEnter;

    private final List<ChildItem> childItems = new ArrayList<>();
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);

        spinnerChildren = findViewById(R.id.spinnerChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        btnEnter = findViewById(R.id.btnEnter);

        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        
        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, R.string.child_qr_invalid, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEnterClicked();
            }
        });

        loadChildren(parentId);
    }

    // קריאה מ-Firebase כדי למלא את ה-Spinner בשמות הילדים של ההורה
    private void loadChildren(String selectedParentId) {
        FirebaseDatabase.getInstance().getReference("parents").child(selectedParentId).child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childItems.clear();
                // עוברים על כל הילדים שהתקבלו מ-Firebase
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String childId = snap.getKey();
                    String firstName = snap.child("firstName").getValue(String.class);
                    String lastName = snap.child("lastName").getValue(String.class);
                    childItems.add(new ChildItem(childId, getFullName(firstName, lastName, getString(R.string.default_child_name_fallback))));
                }
                
                if (childItems.isEmpty()) {
                    tvNoChildren.setVisibility(View.VISIBLE);
                    spinnerChildren.setVisibility(View.GONE);
                    btnEnter.setEnabled(false);
                    return;
                }
                
                tvNoChildren.setVisibility(View.GONE);
                spinnerChildren.setVisibility(View.VISIBLE);
                btnEnter.setEnabled(true);
                
                List<String> names = new ArrayList<>();
                for (int i = 0; i < childItems.size(); i++) {
                    ChildItem item = childItems.get(i);
                    names.add(item.name);
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ChildSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, names);
                spinnerChildren.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildSelectionActivity.this, "הפעולה נכשלה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEnterClicked() {
        int selectedPosition = spinnerChildren.getSelectedItemPosition();
        if (selectedPosition < 0) {
            Toast.makeText(this, R.string.child_selection_please_select, Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "התחברות הצליחה!", Toast.LENGTH_SHORT).show();
        
        // מעביר את ה-ID של הילד ושל ההורה לדשבורד הילד
        String childId = childItems.get(selectedPosition).id;
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra(ChildDashboardActivity.EXTRA_PARENT_ID, parentId);
        intent.putExtra(ChildDashboardActivity.EXTRA_CHILD_ID, childId);
        startActivity(intent);
        finish();
    }

    private String getFullName(String firstName, String lastName, String defaultName) {
        String fullName = "";
        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName = firstName.trim();
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (!fullName.isEmpty()) {
                fullName += " ";
            }
            fullName += lastName.trim();
        }
        if (fullName.isEmpty()) {
            return defaultName;
        }
        return fullName;
    }

    // מחלקת עזר קטנה לשמירת id ושם של ילד
    private static class ChildItem {
        String id, name;

        ChildItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
