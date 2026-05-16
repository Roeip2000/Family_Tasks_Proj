package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

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

    // שתי הרשימות נבנות באותו סדר: שם להצגה ומזהה אמיתי
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

        // קבלת מזהה ההורה שהגיע ממסך סריקת ה-QR
        parentId = getIntent().getStringExtra("parentId");

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEnterClicked();
            }
        });

        // הכפתור לא פעיל עד שהילדים נטענים
        btnEnter.setEnabled(false);

        loadChildren();
    }

    // טעינת הילדים של ההורה מתוך Firebase
    private
    void loadChildren()
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

                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            String id = childSnapshot.getKey();
                            String childName = childSnapshot.child("firstName").getValue(String.class);

                            childIds.add(id);
                            childNames.add(childName);
                        }

                        if (childIds.isEmpty()) {
                            return;
                        }

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
                        // Firebase מחייב לממש את הפעולה הזאת
                    }
                });
    }

    // מעבר לדשבורד של הילד שנבחר
    private void onEnterClicked()
    {

        int selectedPosition = spinnerChildren.getSelectedItemPosition();

        // לפי המיקום שנבחר ב-Spinner מוצאים את ה-id של הילד
        String childId = childIds.get(selectedPosition);

        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra(ChildDashboardActivity.EXTRA_PARENT_ID, parentId);
        intent.putExtra(ChildDashboardActivity.EXTRA_CHILD_ID, childId);
        startActivity(intent);
        finish();
    }
}