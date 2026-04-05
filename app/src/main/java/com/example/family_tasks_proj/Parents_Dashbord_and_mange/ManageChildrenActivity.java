package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.Class_child.Child;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך ניהול ילדים — הוספה, צפייה, עריכה ומחיקה.
 *
 * המסך מחולק לשני חלקים:
 * - למעלה: טופס הוספת ילד חדש (שם פרטי + משפחה + כפתור).
 * - למטה: רשימת כל הילדים הרשומים של ההורה.
 *
 * לחיצה על ילד ברשימה פותחת תפריט עם שתי אפשרויות:
 * 1. ערוך שם — פותח דיאלוג עם שדות שם חדש.
 * 2. מחק ילד — מבקש אישור ומוחק מ-Firebase (כולל כל המשימות שלו).
 *
 * הרשימה מתעדכנת אוטומטית אחרי כל פעולה (הוספה / עריכה / מחיקה).
 *
 * Layout: activity_manage_children.xml
 * נתיב Firebase: /parents/{uid}/children/
 */
public class ManageChildrenActivity extends AppCompatActivity {

    // --- Views ---
    private EditText etFirstName, etLastName;
    private Button btnAddChild;
    private ListView lvChildren;
    private TextView tvNoChildren;

    // --- Firebase ---
    private DatabaseReference db;
    private String parentUID;

    // --- נתוני רשימת ילדים ---
    /** רשימת ילדים — כל אחד מכיל id, firstName, lastName */
    private final List<ChildItem> childItems = new ArrayList<>();
    /** שמות לתצוגה ב-ListView — האינדקס תואם ל-childItems */
    private final List<String> childDisplayNames = new ArrayList<>();
    private ArrayAdapter<String> childAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        // חיבור שדות טופס ההוספה
        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        btnAddChild = findViewById(R.id.btnAddChild);

        // חיבור רשימת ילדים
        lvChildren   = findViewById(R.id.lvChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);

        // reference ל-root של Firebase
        db = FirebaseDatabase.getInstance().getReference();

        // בדיקה שההורה מחובר
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "אינך מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        parentUID = currentUser.getUid();

        // הגדרת ListView עם ArrayAdapter מובנה של Android
        childAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, childDisplayNames);
        lvChildren.setAdapter(childAdapter);

        // לחיצה על ילד ברשימה → תפריט ערוך/מחק
        lvChildren.setOnItemClickListener((parent, view, position, id) ->
                showChildOptionsDialog(position));

        // כפתור הוספת ילד
        btnAddChild.setOnClickListener(v -> addChild());

        // טעינת רשימת הילדים מ-Firebase
        loadChildren();
    }

    // =====================================================================
    //  הוספת ילד חדש
    // =====================================================================

    /**
     * שומר ילד חדש ב-Firebase ומרענן את הרשימה.
     *
     * תהליך:
     * 1. בודק שהשדות מלאים.
     * 2. push().getKey() — מזהה ייחודי.
     * 3. setValue — כותב ל-Firebase.
     * 4. מנקה שדות + טוען מחדש את הרשימה.
     *
     * נתיב כתיבה: /parents/{parentUID}/children/{childId}
     */
    private void addChild() {
        String first = etFirstName.getText().toString().trim();
        String last  = etLastName.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מזהה ייחודי
        String childId = db.child("parents").child(parentUID)
                .child("children").push().getKey();
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
                    btnAddChild.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(ManageChildrenActivity.this,
                                first + " " + last + " נוסף בהצלחה!",
                                Toast.LENGTH_SHORT).show();
                        etFirstName.setText("");
                        etLastName.setText("");
                        etFirstName.requestFocus();

                        // רענון הרשימה — הילד החדש יופיע
                        loadChildren();
                    } else {
                        Toast.makeText(this,
                                "שגיאה: " + (task.getException() != null
                                        ? task.getException().getMessage() : "לא ידועה"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // =====================================================================
    //  טעינת רשימת ילדים מ-Firebase
    // =====================================================================

    /**
     * קורא את כל הילדים מ-/parents/{parentUID}/children/ ומציג ב-ListView.
     * מטפל גם במצב ריק (אין ילדים — מציג הודעה).
     */
    private void loadChildren() {
        db.child("parents").child(parentUID).child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        childItems.clear();
                        childDisplayNames.clear();

                        // עוברים על כל הילדים ושומרים את הנתונים
                        for (DataSnapshot childSnap : snapshot.getChildren()) {
                            String cId = childSnap.getKey();
                            if (cId == null) continue;

                            String first = childSnap.child("firstName").getValue(String.class);
                            String last  = childSnap.child("lastName").getValue(String.class);

                            childItems.add(new ChildItem(cId, first, last));
                            childDisplayNames.add(
                                    NameUtils.fullNameOrDefault(first, last, "ילד"));
                        }

                        // עדכון ה-ListView
                        childAdapter.notifyDataSetChanged();

                        // הצגה/הסתרה של הודעת "אין ילדים"
                        if (childItems.isEmpty()) {
                            tvNoChildren.setVisibility(View.VISIBLE);
                            lvChildren.setVisibility(View.GONE);
                        } else {
                            tvNoChildren.setVisibility(View.GONE);
                            lvChildren.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ManageChildrenActivity.this,
                                "שגיאה בטעינת ילדים: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =====================================================================
    //  תפריט פעולות לילד (ערוך / מחק)
    // =====================================================================

    /**
     * פותח תפריט עם "ערוך שם" / "מחק ילד".
     * נקרא כשלוחצים על ילד ברשימה.
     */
    private void showChildOptionsDialog(int position) {
        if (position < 0 || position >= childItems.size()) return;

        String name = childDisplayNames.get(position);

        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(new String[]{"ערוך שם", "מחק ילד"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditChildDialog(position);
                    } else {
                        showDeleteChildDialog(position);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =====================================================================
    //  עריכת שם ילד
    // =====================================================================

    /**
     * פותח דיאלוג עם שדות שם פרטי + משפחה (ממולאים מראש בשם הנוכחי).
     * אחרי שמירה — מעדכן ב-Firebase ומרענן את הרשימה.
     *
     * נתיב עדכון: /parents/{parentUID}/children/{childId}
     * משתמש ב-updateChildren() כדי לא לדרוס שדות אחרים (כמו tasks).
     */
    private void showEditChildDialog(int position) {
        ChildItem item = childItems.get(position);

        // בניית layout לדיאלוג עם שני שדות
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 0);

        EditText etNewFirst = new EditText(this);
        etNewFirst.setHint("שם פרטי");
        if (item.firstName != null) etNewFirst.setText(item.firstName);
        layout.addView(etNewFirst);

        EditText etNewLast = new EditText(this);
        etNewLast.setHint("שם משפחה");
        if (item.lastName != null) etNewLast.setText(item.lastName);
        layout.addView(etNewLast);

        new AlertDialog.Builder(this)
                .setTitle("עריכת שם ילד")
                .setView(layout)
                .setPositiveButton("שמור", (dialog, which) -> {
                    String newFirst = etNewFirst.getText().toString().trim();
                    String newLast  = etNewLast.getText().toString().trim();

                    if (newFirst.isEmpty() || newLast.isEmpty()) {
                        Toast.makeText(this, "יש למלא את כל השדות",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // עדכון ב-Firebase — updateChildren כדי לא לדרוס tasks
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("firstName", newFirst);
                    updates.put("lastName", newLast);

                    db.child("parents").child(parentUID)
                            .child("children").child(item.id)
                            .updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "השם עודכן!",
                                        Toast.LENGTH_SHORT).show();
                                loadChildren(); // רענון הרשימה
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "שגיאה: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =====================================================================
    //  מחיקת ילד
    // =====================================================================

    /**
     * מבקש אישור מההורה ומוחק את הילד מ-Firebase.
     * שים לב: מחיקת הילד מוחקת גם את כל המשימות שלו (כי הן תחת אותו node).
     *
     * נתיב מחיקה: /parents/{parentUID}/children/{childId} (כולל tasks/)
     */
    private void showDeleteChildDialog(int position) {
        ChildItem item = childItems.get(position);
        String name = childDisplayNames.get(position);

        new AlertDialog.Builder(this)
                .setTitle("מחיקת ילד")
                .setMessage("למחוק את " + name + "?\nכל המשימות שלו יימחקו גם!")
                .setPositiveButton("מחק", (dialog, which) ->
                        db.child("parents").child(parentUID)
                                .child("children").child(item.id)
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, name + " נמחק",
                                            Toast.LENGTH_SHORT).show();
                                    loadChildren(); // רענון הרשימה
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "שגיאה: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("ביטול", null)
                .show();
    }

    // =====================================================================
    //  מחלקה פנימית — מחזיקה נתוני ילד מהרשימה
    // =====================================================================

    /**
     * שומרת id + שם פרטי + שם משפחה של ילד.
     * משמשת את הרשימה כדי שנדע איזה ילד לערוך/למחוק בלי לקרוא שוב מ-Firebase.
     */
    private static class ChildItem {
        final String id;
        final String firstName;
        final String lastName;

        ChildItem(String id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
