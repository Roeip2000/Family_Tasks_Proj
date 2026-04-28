package com.example.family_tasks_proj.Parents_Dashbord_and_mange;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.model.Child;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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
 * מסך ניהול הילדים (הוספה, עריכה, מחיקה).
 * הקוד בנוי בצורה מודולרית כדי שיהיה קל להוסיף שדות או לשנות לוגיקה בזמן הבחינה.
 */
public class ManageChildrenActivity extends AppCompatActivity {

    // --- 1. הגדרת משתני הממשק (UI) ---
    private EditText etFirstName, etLastName;
    private Button btnAddChild, btnCancelEdit, btnBack;
    private ListView lvChildren;
    private TextView tvNoChildren, tvFormTitle;

    // --- 2. משתני עזר לנתונים ---
    private final List<ChildItem> childItems = new ArrayList<>();
    private ChildListAdapter childListAdapter;
    private String parentUid;
    private String editingChildId = null; // מכיל ID אם אנחנו במצב עריכה, אחרת null

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        // שלב א: זיהוי ההורה המחובר
        parentUid = FirebaseAuth.getInstance().getUid();
        if (parentUid == null) {
            Toast.makeText(this, "שגיאה: אינך מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // שלב ב: אתחול הממשק והמאזינים
        initViews();
        setupListeners();
        setupListView();
        
        // שלב ג: טעינת נתונים ראשונית מהענן
        loadChildrenFromFirebase();
    }

    // --- אתחול רכיבי המסך (findViewById) ---
    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnAddChild = findViewById(R.id.btnAddChild);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvChildren = findViewById(R.id.lvChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        tvFormTitle = findViewById(R.id.tvFormTitle);
    }

    // --- הגדרת מאזינים ללחיצות (Listeners) ---
    private void setupListeners() {
        btnAddChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processSaveChild(); // מבצע שמירה או עדכון
            }
        });

        btnCancelEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitEditMode(); // מבטל את מצב העריכה
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // חוזר למסך הקודם
            }
        });
    }

    // --- הגדרת הרשימה (ListView + Adapter) ---
    private void setupListView() {
        childListAdapter = new ChildListAdapter();
        lvChildren.setAdapter(childListAdapter);
        lvChildren.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showChildOptions(position); // פותח תפריט אפשרויות לילד
            }
        });
    }

    // --- לוגיקה: משיכת נתונים מה-Firebase ---
    private void loadChildrenFromFirebase() {
        // --- נושא במחוון: Firebase Realtime Database ---
        getDbRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childItems.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot childSnap : snapshot.getChildren()) {
                        String id = childSnap.getKey();
                        String fName = childSnap.child("firstName").getValue(String.class);
                        String lName = childSnap.child("lastName").getValue(String.class);
                        childItems.add(new ChildItem(id, fName, lName));
                    }
                }
                refreshUI(); // מעדכן את המסך
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- לוגיקה: שמירה או עדכון של ילד ---
    private void processSaveChild() {
        final String fName = etFirstName.getText().toString().trim();
        final String lName = etLastName.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מזהה (ID) - אם אנחנו בעריכה נשתמש בקיים, אם לא ניצור חדש ב-Firebase
        final String childId = (editingChildId != null) ? editingChildId : getDbRef().push().getKey();
        if (childId == null) return;

        // הכנת הנתונים לשמירה במבנה של מפה (Key-Value)
        Map<String, Object> childData = new HashMap<>();
        childData.put("firstName", fName);
        childData.put("lastName", lName);

        btnAddChild.setEnabled(false);
        // ביצוע השמירה ב-Firebase
        getDbRef().child(childId).updateChildren(childData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                btnAddChild.setEnabled(true);
                Toast.makeText(ManageChildrenActivity.this, "הנתונים נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
                exitEditMode();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                btnAddChild.setEnabled(true);
                Toast.makeText(ManageChildrenActivity.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- לוגיקה: מחיקת ילד ---
    private void deleteChild(String id) {
        getDbRef().child(id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ManageChildrenActivity.this, "הילד נמחק מהמערכת", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- ניהול מצבי תצוגה (עריכה / הוספה) ---
    private void enterEditMode(ChildItem item) {
        editingChildId = item.id;
        etFirstName.setText(item.firstName);
        etLastName.setText(item.lastName);
        btnAddChild.setText("שמור שינויים");
        btnCancelEdit.setVisibility(View.VISIBLE);
        tvFormTitle.setText("עריכת פרטי ילד");
    }

    private void exitEditMode() {
        editingChildId = null;
        etFirstName.setText("");
        etLastName.setText("");
        btnAddChild.setText("הוסף ילד");
        btnCancelEdit.setVisibility(View.GONE);
        tvFormTitle.setText("הוספת ילד חדש");
    }

    private void refreshUI() {
        childListAdapter.notifyDataSetChanged();
        updateListViewHeight(lvChildren);
        tvNoChildren.setVisibility(childItems.isEmpty() ? View.VISIBLE : View.GONE);
        lvChildren.setVisibility(childItems.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showChildOptions(int position) {
        final ChildItem item = childItems.get(position);
        String[] options = {"ערוך פרטים", "מחק ילד"};
        new AlertDialog.Builder(this)
                .setTitle("בחר פעולה עבור " + item.firstName)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) enterEditMode(item);
                        else showDeleteConfirm(item);
                    }
                }).show();
    }

    private void showDeleteConfirm(final ChildItem item) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת ילד")
                .setMessage("האם למחוק את " + item.firstName + "? פעולה זו תמחק גם את כל המשימות שלו.")
                .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteChild(item.id);
                    }
                })
                .setNegativeButton("ביטול", null).show();
    }

    private DatabaseReference getDbRef() {
        return FirebaseDatabase.getInstance().getReference("parents").child(parentUid).child("children");
    }

    // מתאם להצגת שורות ב-ListView
    private class ChildListAdapter extends ArrayAdapter<ChildItem> {
        ChildListAdapter() { super(ManageChildrenActivity.this, 0, childItems); }
        @NonNull @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_manage_child, parent, false);
            ChildItem item = getItem(position);
            if (item != null) {
                ((TextView) convertView.findViewById(R.id.tvChildFullName)).setText(item.firstName + " " + item.lastName);
            }
            return convertView;
        }
    }

    private static class ChildItem {
        String id, firstName, lastName;
        ChildItem(String id, String f, String l) { this.id = id; this.firstName = f; this.lastName = l; }
    }

    private void updateListViewHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) return;
        int totalHeight = 0;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listView.getWidth() > 0 ? listView.getWidth() : getResources().getDisplayMetrics().widthPixels - 100, View.MeasureSpec.AT_MOST);
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
    }
}
