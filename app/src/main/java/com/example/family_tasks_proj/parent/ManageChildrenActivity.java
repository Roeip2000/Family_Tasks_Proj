package com.example.family_tasks_proj.parent;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etChildName;
    private Button btnSaveChild, btnBack;
    private ListView lvChildren;

    // childNames מוצג במסך, childIds שומר את המזהים של הילדים ב-Firebase
    private final List<String> childIds = new ArrayList<>();
    private final List<String> childNames = new ArrayList<>();

    private ArrayAdapter<String> listAdapter;
    private String parentId;
    private DatabaseReference childrenReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        // קבלת מזהה ההורה המחובר
        parentId = FirebaseAuth.getInstance().getUid();

        // הנתיב לילדים של ההורה המחובר ב-Firebase
        childrenReference = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children");

        etChildName = findViewById(R.id.etFirstName);
        btnSaveChild = findViewById(R.id.btnAddChild);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvChildren = findViewById(R.id.lvChildren);

        // הגדרת המתאם לרשימה
        listAdapter = new ArrayAdapter<>(this,
                R.layout.item_manage_child,
                R.id.tvChildFullName,
                childNames
        );
        lvChildren.setAdapter(listAdapter);

        // לחיצה על כפתור שמירה
        btnSaveChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog();
            }
        });

        // חזרה למסך הקודם
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadChildrenFromFirebase();
    }

    // טעינת הילדים מ-Firebase
    private void loadChildrenFromFirebase() {
        childrenReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childIds.clear();
                childNames.clear();

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childId = childSnapshot.getKey();
                    String firstName = childSnapshot.child("firstName").getValue(String.class);

                    childIds.add(childId);
                    childNames.add(firstName);
                }

                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // הצגת דיאלוג אישור לפני שמירה
    private void showConfirmDialog() {
        String name = etChildName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("אישור הוספה");
        builder.setMessage("האם אתה בטוח שהפרטים נכונים?");
        
        builder.setPositiveButton("כן, שמור", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveChild(name);
            }
        });
        
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    // שמירת ילד חדש ב-Firebase
    private void saveChild(String firstName) {
        // יצירת מזהה חדש ב-Firebase
        String childId = childrenReference.push().getKey();

        // שמירת השם ב-Firebase תחת המזהה החדש
        childrenReference.child(childId).child("firstName").setValue(firstName);
        
        Toast.makeText(ManageChildrenActivity.this, R.string.toast_child_saved, Toast.LENGTH_SHORT).show();
        
        // ניקוי הטקסט אחרי שמירה
        etChildName.setText("");
    }
}
