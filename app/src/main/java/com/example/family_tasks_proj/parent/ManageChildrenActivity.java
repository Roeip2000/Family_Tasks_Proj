package com.example.family_tasks_proj.parent;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

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

// מסך הוספת ילד וצפייה ברשימת הילדים של ההורה
public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etChildName;
    private Button btnAddChild, btnBack;
    private ListView lvChildren;

    // רשימת שמות הילדים שמוצגת במסך
    private final List<String> childNames = new ArrayList<>();

    private ArrayAdapter<String> listAdapter;
    private String parentId;
    private DatabaseReference childrenReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        // מזהה ההורה המחובר
        parentId = FirebaseAuth.getInstance().getUid();

        // הנתיב לילדים של ההורה ב-Firebase
        childrenReference = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children");

        // חיבור רכיבי המסך מה-XML לקוד
        etChildName = findViewById(R.id.etFirstName);
        btnAddChild = findViewById(R.id.btnAddChild);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvChildren = findViewById(R.id.lvChildren);

        // המתאם מציג את שמות הילדים ברשימה
        listAdapter = new ArrayAdapter<>(this,
                R.layout.item_manage_child,
                R.id.tvChildFullName,
                childNames
        );
        lvChildren.setAdapter(listAdapter);

        // לחיצה על "הוסף ילד" מוסיפה ילד חדש
        btnAddChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addChild();
            }
        });

        // לחיצה על "חזרה" סוגרת את המסך
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // טעינת הילדים מ-Firebase והצגתם ברשימה
        loadChildrenFromFirebase();
    }

    // טעינת שמות הילדים מ-Firebase והצגתם ברשימה
    private void loadChildrenFromFirebase() {
        childrenReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // בונים מחדש את הרשימה בכל שינוי בנתונים
                childNames.clear();

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String firstName = childSnapshot.child("firstName").getValue(String.class);
                    childNames.add(firstName);
                }

                // מעדכנים את התצוגה ברשימה
                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // הוספת ילד חדש ל-Firebase
    private void addChild() {
        // קריאת השם שההורה הקליד
        String firstName = etChildName.getText().toString().trim();

        // בדיקה שהשדה לא ריק
        if (firstName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מזהה ייחודי חדש לילד
        String childId = childrenReference.push().getKey();

        // שמירת השם תחת המזהה החדש
        childrenReference.child(childId).child("firstName").setValue(firstName).addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ManageChildrenActivity.this, R.string.toast_child_saved, Toast.LENGTH_SHORT).show();
                // ניקוי השדה לקראת הילד הבא
                etChildName.setText("");
            }
        });
    }
}
