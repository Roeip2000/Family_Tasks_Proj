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

    private Spinner spinnerChildren;
    private Button btnEnter;

    // שתי הרשימות נשמרות באותו סדר
    private final List<String> childIds = new ArrayList<>();
    private final List<String> childNames = new ArrayList<>();

    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);

        spinnerChildren = findViewById(R.id.spinnerChildren);
        btnEnter = findViewById(R.id.btnEnter);

        // מזהה ההורה מגיע מסריקת ה-QR
        parentId = getIntent().getStringExtra(ChildDashboardActivity.EXTRA_PARENT_ID);
        if (parentId == null || parentId.trim().isEmpty()) {
            Toast.makeText(this, R.string.error_action_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadChildren();

        // לחיצה על "כניסה" מעבירה את הילד לדשבורד שלו
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEnterClicked();
            }
        });
    }

    // טעינת הילדים של ההורה
    private void loadChildren()
    {
        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        childIds.clear();
                        childNames.clear();
                        childNames.add("בחר ילד");

                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            String childId = childSnapshot.getKey();
                            String firstName = childSnapshot.child("firstName").getValue(String.class);

                            childIds.add(childId);
                            childNames.add(firstName);
                        }

                        // הצגת שמות הילדים
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                ChildSelectionActivity.this,
                                android.R.layout.simple_spinner_item,
                                childNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerChildren.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    // מעבר לדשבורד של הילד
    private void onEnterClicked()
    {
        int selectedPosition = spinnerChildren.getSelectedItemPosition();

        // בדיקה שהילד לא נשאר על אפשרות ברירת המחדל
        if (selectedPosition <= 0) {
            Toast.makeText(this, R.string.empty_no_children_selection, Toast.LENGTH_SHORT).show();
            return;
        }

        // מחסירים 1 כי הוספנו את "בחר ילד" במקום ה-0
        String childId = childIds.get(selectedPosition - 1);

        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra(ChildDashboardActivity.EXTRA_PARENT_ID, parentId);
        intent.putExtra(ChildDashboardActivity.EXTRA_CHILD_ID, childId);
        startActivity(intent);
        finish();
    }
}
