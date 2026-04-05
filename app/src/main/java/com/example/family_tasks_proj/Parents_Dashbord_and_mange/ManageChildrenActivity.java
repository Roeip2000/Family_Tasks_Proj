package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.child.Class_child.Child;
import com.example.family_tasks_proj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * מסך הוספת ילד חדש.
 *
 * אחריות:
 * - מקבל שם פרטי + שם משפחה מהמשתמש.
 * - יוצר רשומת ילד ב-Firebase תחת /parents/{uid}/children/{childId}.
 * - בהצלחה — מציג הודעה, מנקה את השדות, ונשאר במסך להוספת ילדים נוספים.
 *
 * Layout: activity_manage_children.xml
 *
 * ===== הערות לשיפור =====
 * TODO: להוסיף רשימת ילדים קיימים מתחת לטופס ההוספה (ListView/RecyclerView).
 *       כרגע המסך מאפשר רק הוספה — אין צפייה ברשימה, עריכה, או מחיקה.
 * TODO: להוסיף ולידציה (למשל: אורך שם מינימלי, תווים חוקיים בלבד).
 */
public class ManageChildrenActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName;
    private Button btnAddChild;

    private DatabaseReference db;
    private String parentUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        // חיבור שדות מה-layout
        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        btnAddChild = findViewById(R.id.btnAddChild);

        // reference ל-root של Firebase
        db = FirebaseDatabase.getInstance().getReference();

        // בדיקה שההורה מחובר — אחרת אין מה לעשות
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "אינך מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        parentUID = currentUser.getUid();

        btnAddChild.setOnClickListener(v -> addChild());
    }

    /**
     * שומר ילד חדש ב-Firebase.
     *
     * תהליך:
     * 1. ולידציה — בודק שהשדות מלאים.
     * 2. push().getKey() — מייצר מזהה ייחודי לילד.
     * 3. setValue — כותב את אובייקט Child ל-Firebase.
     * 4. בהצלחה — מציג Toast, מנקה שדות, נשאר במסך להוספת ילדים נוספים.
     *
     * נתיב כתיבה: /parents/{parentUID}/children/{childId}
     * Side-effect: משבית את הכפתור בזמן הכתיבה למניעת כפילויות.
     */
    private void addChild() {
        String first = etFirstName.getText().toString().trim();
        String last  = etLastName.getText().toString().trim();

        // ולידציה — שדות ריקים
        if (first.isEmpty() || last.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מזהה ייחודי לילד עם push()
        String childId = db.child("parents").child(parentUID).child("children").push().getKey();
        if (childId == null) {
            Toast.makeText(this, "שגיאה ביצירת מזהה ילד", Toast.LENGTH_SHORT).show();
            return;
        }

        Child newChild = new Child(first, last);

        // משבית כפתור למניעת לחיצות כפולות
        btnAddChild.setEnabled(false);

        db.child("parents")
                .child(parentUID)
                .child("children")
                .child(childId)
                .setValue(newChild)
                .addOnCompleteListener(task -> {
                    btnAddChild.setEnabled(true); // מאפשר לחיצה מחדש

                    if (task.isSuccessful()) {
                        // הצלחה — מציג הודעה, מנקה שדות, נשאר במסך להוספת ילדים נוספים
                        Toast.makeText(ManageChildrenActivity.this,
                                first + " " + last + " נוסף בהצלחה!",
                                Toast.LENGTH_SHORT).show();
                        etFirstName.setText("");
                        etLastName.setText("");
                        etFirstName.requestFocus();
                    } else {
                        Toast.makeText(this,
                                "שגיאה: " + (task.getException() != null ? task.getException().getMessage() : "לא ידועה"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
