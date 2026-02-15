package com.example.family_tasks_proj.Child_Login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך בחירת הורה + ילד — מוצג לפני דשבורד הילד.
 *
 * אחריות:
 * - אם parentId ידוע (מ-Intent/SharedPreferences): מציג רק Spinner ילדים.
 * - אם parentId לא ידוע (כניסה ישירה בלי סשן): מציג Spinner הורים קודם,
 *   אחרי בחירת הורה טוען את הילדים שלו ב-Spinner השני.
 * - אחרי לחיצה "כניסה" — שומר סשן ופותח ChildDashboardActivity.
 *
 * Layout: activity_child_selection.xml
 *
 * ===== ניווט =====
 * כניסה מ: ChildQRLoginFragment (אחרי סריקה), או MainActivity (כפתור "כניסה ישירה")
 * יציאה ל: ChildDashboardActivity (עם parentId + childId)
 *
 * ===== נתיבי Firebase =====
 * קריאה מ: /parents/ — טוען את כל ההורים (כשצריך לבחור הורה)
 * קריאה מ: /parents/{parentId}/children/ — טוען ילדים של הורה ספציפי
 */
public class ChildSelectionActivity extends AppCompatActivity {

    private static final String TAG = "ChildSelection";

    // מפתחות SharedPreferences לסשן הילד
    private static final String PREFS = "child_session";
    private static final String KEY_PARENT = "parentId";
    private static final String KEY_CHILD = "childId";

    // --- Views ---
    private TextView tvParentLabel;
    private Spinner spinnerParents;
    private TextView tvChildLabel;
    private Spinner spinnerChildren;
    private TextView tvNoChildren;
    private Button btnEnter;
    private ProgressBar progressBar;

    /** parentId — ידוע מ-QR/סשן, או null אם צריך לבחור הורה */
    private String parentId;
    /** childId שהגיע מה-QR — אם קיים, נבחר אוטומטית ב-Spinner הילדים */
    private String preselectedChildId;

    /** רשימת הורים — רק כשצריך לבחור הורה */
    private final List<ParentItem> parentList = new ArrayList<>();
    /** רשימת ילדים של ההורה הנבחר */
    private final List<ChildItem> childList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);

        // חיבור views
        tvParentLabel = findViewById(R.id.tvParentLabel);
        spinnerParents = findViewById(R.id.spinnerParents);
        tvChildLabel = findViewById(R.id.tvChildLabel);
        spinnerChildren = findViewById(R.id.spinnerChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        btnEnter = findViewById(R.id.btnEnter);
        progressBar = findViewById(R.id.progressBar);

        // קבלת parentId ו-childId מ-Intent (אחרי QR) או מ-SharedPreferences
        resolveIds();

        if (parentId != null && !parentId.isEmpty()) {
            // parentId ידוע — מסתירים Spinner הורים, טוענים ישר ילדים
            tvParentLabel.setVisibility(View.GONE);
            spinnerParents.setVisibility(View.GONE);
            loadChildren(parentId);
        } else {
            // parentId לא ידוע — מציגים Spinner הורים, ילדים ייטענו אחרי בחירה
            tvParentLabel.setVisibility(View.VISIBLE);
            spinnerParents.setVisibility(View.VISIBLE);
            tvChildLabel.setVisibility(View.GONE);
            spinnerChildren.setVisibility(View.GONE);
            btnEnter.setEnabled(false);
            loadParents();
        }

        // כפתור כניסה
        btnEnter.setOnClickListener(v -> onEnterClicked());
    }

    /**
     * קובע parentId + preselectedChildId:
     * 1. קודם מ-Intent extras (אחרי סריקת QR)
     * 2. אם חסר — מ-SharedPreferences (כניסה ישירה עם סשן קודם)
     * 3. אם עדיין חסר — parentId יישאר null → נטען Spinner הורים
     */
    private void resolveIds() {
        Intent i = getIntent();
        if (i != null) {
            parentId = i.getStringExtra("parentId");
            preselectedChildId = i.getStringExtra("childId");
        }
        // fallback ל-SharedPreferences
        if (parentId == null || parentId.isEmpty()) {
            SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            parentId = sp.getString(KEY_PARENT, null);
            preselectedChildId = sp.getString(KEY_CHILD, null);
        }
        Log.d(TAG, "resolveIds: parentId=" + parentId + ", preselectedChildId=" + preselectedChildId);
    }

    // =====================================================================
    //  טעינת הורים מ-Firebase: /parents/
    // =====================================================================

    /**
     * טוען את כל ההורים מ-Firebase בנתיב /parents/.
     * ממלא את spinnerParents ומגדיר listener — כשבוחרים הורה, טוענים את הילדים שלו.
     */
    private void loadParents() {
        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference parentsRef = FirebaseDatabase.getInstance()
                .getReference("parents");

        parentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                parentList.clear();

                // עוברים על כל ההורים ובונים רשימה
                for (DataSnapshot parentSnap : snapshot.getChildren()) {
                    String uid = parentSnap.getKey();
                    String firstName = parentSnap.child("firstName").getValue(String.class);
                    String lastName = parentSnap.child("lastName").getValue(String.class);

                    // בניית שם מלא — משתמש ב-NameUtils
                    String fallback = "הורה (" + uid.substring(0, Math.min(uid.length(), 6)) + ")";
                    String fullName = NameUtils.fullNameOrDefault(firstName, lastName, fallback);
                    parentList.add(new ParentItem(uid, fullName));
                }

                if (parentList.isEmpty()) {
                    Toast.makeText(ChildSelectionActivity.this,
                            "אין הורים רשומים במערכת", Toast.LENGTH_LONG).show();
                    return;
                }

                populateParentSpinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Firebase error loading parents: " + error.getMessage());
                Toast.makeText(ChildSelectionActivity.this,
                        "שגיאה בטעינת הורים: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * ממלא את spinnerParents בשמות ההורים.
     * מגדיר OnItemSelectedListener — כשבוחרים הורה, טוענים את הילדים שלו.
     */
    private void populateParentSpinner() {
        List<String> names = new ArrayList<>();
        for (ParentItem item : parentList) {
            names.add(item.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerParents.setAdapter(adapter);

        // כשבוחרים הורה — טוענים את הילדים שלו
        spinnerParents.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                if (pos < 0 || pos >= parentList.size()) return;
                ParentItem selected = parentList.get(pos);
                parentId = selected.id;
                Log.d(TAG, "Parent selected: " + selected.name + " (" + selected.id + ")");

                // מציגים את אזור בחירת ילד וטוענים מ-Firebase
                tvChildLabel.setVisibility(View.VISIBLE);
                spinnerChildren.setVisibility(View.VISIBLE);
                loadChildren(parentId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // לא נבחר כלום — מסתירים ילדים
                tvChildLabel.setVisibility(View.GONE);
                spinnerChildren.setVisibility(View.GONE);
                btnEnter.setEnabled(false);
            }
        });
    }

    // =====================================================================
    //  טעינת ילדים מ-Firebase: /parents/{parentId}/children/
    // =====================================================================

    /**
     * טוען את רשימת הילדים מ-Firebase: /parents/{parentId}/children/
     * מעדכן את spinnerChildren ומטפל במצב ריק (אין ילדים).
     *
     * @param forParentId ה-parentId שממנו טוענים ילדים
     */
    private void loadChildren(String forParentId) {
        progressBar.setVisibility(View.VISIBLE);
        btnEnter.setEnabled(false);

        DatabaseReference childrenRef = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(forParentId)
                .child("children");

        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                childList.clear();

                // עוברים על כל הילדים ובונים רשימה
                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String childId = childSnap.getKey();
                    String firstName = childSnap.child("firstName").getValue(String.class);
                    String lastName = childSnap.child("lastName").getValue(String.class);

                    // בניית שם מלא — משתמש ב-NameUtils
                    String fullName = NameUtils.fullNameOrDefault(firstName, lastName, "ילד (" + childId + ")");
                    childList.add(new ChildItem(childId, fullName));
                }

                if (childList.isEmpty()) {
                    // אין ילדים — מציג הודעה
                    tvNoChildren.setVisibility(View.VISIBLE);
                    spinnerChildren.setVisibility(View.GONE);
                    btnEnter.setEnabled(false);
                    return;
                }

                // יש ילדים — מעדכן Spinner
                tvNoChildren.setVisibility(View.GONE);
                spinnerChildren.setVisibility(View.VISIBLE);
                btnEnter.setEnabled(true);
                populateChildSpinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Firebase error loading children: " + error.getMessage());
                Toast.makeText(ChildSelectionActivity.this,
                        "שגיאה בטעינת ילדים: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * ממלא את spinnerChildren בשמות הילדים.
     * אם יש preselectedChildId (מ-QR) — בוחר אותו אוטומטית.
     */
    private void populateChildSpinner() {
        List<String> names = new ArrayList<>();
        for (ChildItem item : childList) {
            names.add(item.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildren.setAdapter(adapter);

        // בחירה אוטומטית של הילד מה-QR (אם קיים)
        if (preselectedChildId != null && !preselectedChildId.isEmpty()) {
            for (int i = 0; i < childList.size(); i++) {
                if (childList.get(i).id.equals(preselectedChildId)) {
                    spinnerChildren.setSelection(i);
                    break;
                }
            }
        }
    }

    // =====================================================================
    //  כניסה לדשבורד
    // =====================================================================

    /**
     * נקרא בלחיצה על "כניסה".
     * מוודא שנבחרו הורה וילד, שומר סשן, ופותח ChildDashboardActivity.
     */
    private void onEnterClicked() {
        // ולידציה: parentId חייב להיות ידוע (מ-QR, סשן, או Spinner)
        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "בחר הורה מהרשימה", Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spinnerChildren.getSelectedItemPosition();
        if (pos < 0 || pos >= childList.size()) {
            Toast.makeText(this, "בחר ילד מהרשימה", Toast.LENGTH_SHORT).show();
            return;
        }

        ChildItem selected = childList.get(pos);
        Log.d(TAG, "Entering dashboard: parentId=" + parentId + " childId=" + selected.id + " name=" + selected.name);

        // שמירת סשן ב-SharedPreferences — בפעם הבאה לא יצטרך לבחור שוב
        saveSession(parentId, selected.id);

        // פתיחת דשבורד הילד
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra("parentId", parentId);
        intent.putExtra("childId", selected.id);
        startActivity(intent);
        finish();
    }

    /**
     * שומר parentId + childId ב-SharedPreferences.
     * כך בכניסה ישירה הבאה — parentId ידוע, ויעבור ישר ל-Spinner ילדים.
     */
    private void saveSession(String parentId, String childId) {
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_PARENT, parentId)
                .putString(KEY_CHILD, childId)
                .apply();
        Log.d(TAG, "Session saved: parentId=" + parentId + " childId=" + childId);
    }

    // =====================================================================
    //  מחלקות פנימיות
    // =====================================================================

    /** מייצג הורה ברשימה — id (Firebase UID) + name (שם מלא). */
    private static class ParentItem {
        final String id;
        final String name;

        ParentItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** מייצג ילד ברשימה — id (Firebase key) + name (שם מלא). */
    private static class ChildItem {
        final String id;
        final String name;

        ChildItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
