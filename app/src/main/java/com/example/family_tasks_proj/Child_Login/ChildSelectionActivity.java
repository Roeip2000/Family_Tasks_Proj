package com.example.family_tasks_proj.Child_Login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.example.family_tasks_proj.util.NameUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך בחירת ילד.
 * אם parentId הגיע מ-QR או מסשן שמור, הילד בוחר רק את שמו.
 * אם אין parentId, הילד בוחר קודם הורה ואז ילד.
 */
public class ChildSelectionActivity extends AppCompatActivity {

    private static final String PREFS = "child_session";
    private static final String KEY_PARENT = "parentId";
    private static final String KEY_CHILD = "childId";

    private TextView tvParentLabel;
    private TextView tvSubtitle;
    private Spinner spinnerParents;
    private TextView tvChildLabel;
    private Spinner spinnerChildren;
    private TextView tvNoChildren;
    private Button btnEnter;
    private ProgressBar progressBar;

    private final List<ParentItem> parentItems = new ArrayList<>();
    private final List<ChildItem> childItems = new ArrayList<>();

    private String parentId;
    private String preselectedChildId;

    // יוצר את המסך, מוצא מזהים, וטוען הורים או ילדים לפי המצב
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);

        bindViews();
        resolveIds();
        updateSubtitle();
        bindActions();
        openCorrectPicker();
    }

    // מחבר את כל ה-views מה-layout
    private void bindViews() {
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvParentLabel = findViewById(R.id.tvParentLabel);
        spinnerParents = findViewById(R.id.spinnerParents);
        tvChildLabel = findViewById(R.id.tvChildLabel);
        spinnerChildren = findViewById(R.id.spinnerChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        btnEnter = findViewById(R.id.btnEnter);
        progressBar = findViewById(R.id.progressBar);
    }

    // מגדיר את כפתור הכניסה
    private void bindActions() {
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEnterClicked();
            }
        });
    }

    // קודם קורא IDs מה-Intent, ואם אין parentId אז משלים מהסשן המקומי האחרון
    private void resolveIds() {
        Intent intent = getIntent();
        if (intent != null) {
            parentId = intent.getStringExtra(KEY_PARENT);
            preselectedChildId = intent.getStringExtra(KEY_CHILD);
        }

        if (!isBlank(parentId)) {
            return;
        }

        // fallback: אם המסך נפתח בלי QR/extra, משתמשים בסשן הילד האחרון שנשמר ב-SharedPreferences
        SharedPreferences preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        parentId = preferences.getString(KEY_PARENT, null);
        preselectedChildId = preferences.getString(KEY_CHILD, null);
        updateSubtitle();
    }

    // בוחר אם להציג בחירת הורה או רק בחירת ילד
    private void openCorrectPicker() {
        if (isBlank(parentId)) {
            showParentPicker();
            loadParents();
        } else {
            hideParentPicker();
            loadChildren(parentId);
        }
    }

    // מציג בחירת הורה כאשר אין parentId ידוע
    private void showParentPicker() {
        tvParentLabel.setVisibility(View.VISIBLE);
        spinnerParents.setVisibility(View.VISIBLE);
        tvChildLabel.setVisibility(View.GONE);
        spinnerChildren.setVisibility(View.GONE);
        btnEnter.setEnabled(false);
        updateSubtitle();
    }

    // מסתיר בחירת הורה כאשר parentId כבר ידוע
    private void hideParentPicker() {
        tvParentLabel.setVisibility(View.GONE);
        spinnerParents.setVisibility(View.GONE);
        tvChildLabel.setVisibility(View.VISIBLE);
        spinnerChildren.setVisibility(View.VISIBLE);
        updateSubtitle();
    }

    // טוען את רשימת ההורים מ-Firebase: /parents
    private void loadParents() {
        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference parentsRef = FirebaseDatabase.getInstance().getReference("parents");
        parentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleParentsSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showParentsLoadError(error);
            }
        });
    }

    // ממיר snapshot של /parents לרשימת שמות בספינר
    private void handleParentsSnapshot(DataSnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        parentItems.clear();

        for (DataSnapshot parentSnapshot : snapshot.getChildren()) {
            addParentFromSnapshot(parentSnapshot);
        }

        if (parentItems.isEmpty()) {
            Toast.makeText(this, R.string.child_selection_no_parents, Toast.LENGTH_LONG).show();
            return;
        }

        populateParentSpinner();
    }

    // מוסיף הורה אחד מהרשומה שלו ב-Firebase
    private void addParentFromSnapshot(DataSnapshot parentSnapshot) {
        String uid = parentSnapshot.getKey();
        if (isBlank(uid)) {
            return;
        }

        String firstName = parentSnapshot.child("firstName").getValue(String.class);
        String lastName = parentSnapshot.child("lastName").getValue(String.class);
        String shortUid = uid.substring(0, Math.min(uid.length(), 6));
        String fallback = getString(R.string.default_parent_name) + " (" + shortUid + ")";
        String fullName = NameUtils.fullNameOrDefault(firstName, lastName, fallback);
        parentItems.add(new ParentItem(uid, fullName));
    }

    // מציג שגיאה אם טעינת /parents נכשלה
    private void showParentsLoadError(DatabaseError error) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(
                this,
                getString(R.string.child_selection_error_loading_parents, error.getMessage()),
                Toast.LENGTH_LONG
        ).show();
    }

    // ממלא את ספינר ההורים ומחבר מאזין לבחירה
    private void populateParentSpinner() {
        List<String> names = new ArrayList<>();
        for (ParentItem item : parentItems) {
            names.add(item.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                names
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerParents.setAdapter(adapter);
        spinnerParents.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                handleParentSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                clearParentSelection();
            }
        });
    }

    // מטפל בבחירת הורה מתוך הספינר
    private void handleParentSelected(int position) {
        if (position < 0 || position >= parentItems.size()) {
            return;
        }

        parentId = parentItems.get(position).id;
        updateSubtitle();
        tvChildLabel.setVisibility(View.VISIBLE);
        spinnerChildren.setVisibility(View.VISIBLE);
        loadChildren(parentId);
    }

    // מנקה בחירת הורה כאשר אין בחירה בספינר
    private void clearParentSelection() {
        parentId = null;
        btnEnter.setEnabled(false);
        tvChildLabel.setVisibility(View.GONE);
        spinnerChildren.setVisibility(View.GONE);
        updateSubtitle();
    }

    // טוען את הילדים של ההורה שנבחר מ-Firebase: /parents/{parentId}/children
    private void loadChildren(String selectedParentId) {
        progressBar.setVisibility(View.VISIBLE);
        btnEnter.setEnabled(false);

        DatabaseReference childrenRef = FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(selectedParentId)
                .child("children");

        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleChildrenSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showChildrenLoadError(error);
            }
        });
    }

    // ממיר snapshot של /children לרשימת ילדים בספינר
    private void handleChildrenSnapshot(DataSnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        childItems.clear();

        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
            addChildFromSnapshot(childSnapshot);
        }

        updateChildrenPickerVisibility();
    }

    // מוסיף ילד אחד מהרשומה שלו ב-Firebase
    private void addChildFromSnapshot(DataSnapshot childSnapshot) {
        String currentChildId = childSnapshot.getKey();
        if (isBlank(currentChildId)) {
            return;
        }

        String firstName = childSnapshot.child("firstName").getValue(String.class);
        String lastName = childSnapshot.child("lastName").getValue(String.class);
        String fallback = getString(R.string.default_child_name) + " (" + currentChildId + ")";
        String fullName = NameUtils.fullNameOrDefault(firstName, lastName, fallback);
        childItems.add(new ChildItem(currentChildId, fullName));
    }

    // מציג או מסתיר את ספינר הילדים לפי תוצאות הטעינה
    private void updateChildrenPickerVisibility() {
        if (childItems.isEmpty()) {
            tvNoChildren.setVisibility(View.VISIBLE);
            spinnerChildren.setVisibility(View.GONE);
            btnEnter.setEnabled(false);
            return;
        }

        tvNoChildren.setVisibility(View.GONE);
        spinnerChildren.setVisibility(View.VISIBLE);
        btnEnter.setEnabled(true);
        populateChildSpinner();
    }

    // מציג שגיאה אם טעינת הילדים נכשלה
    private void showChildrenLoadError(DatabaseError error) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(
                this,
                getString(R.string.child_selection_error_loading_children, error.getMessage()),
                Toast.LENGTH_LONG
        ).show();
    }

    // ממלא את ספינר הילדים ובוחר ילד מראש אם childId הגיע מהסשן
    private void populateChildSpinner() {
        List<String> names = new ArrayList<>();
        for (ChildItem item : childItems) {
            names.add(item.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                names
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildren.setAdapter(adapter);
        selectSavedChildIfPossible();
    }

    // בוחר בספינר את הילד שנשמר מראש, אם הוא עדיין קיים ברשימה
    private void selectSavedChildIfPossible() {
        if (isBlank(preselectedChildId)) {
            return;
        }

        for (int index = 0; index < childItems.size(); index++) {
            if (preselectedChildId.equals(childItems.get(index).id)) {
                spinnerChildren.setSelection(index);
                break;
            }
        }
    }

    // בודק בחירה, שומר סשן, ופותח את דשבורד הילד
    private void onEnterClicked() {
        if (isBlank(parentId)) {
            Toast.makeText(this, R.string.child_selection_pick_parent, Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = spinnerChildren.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= childItems.size()) {
            Toast.makeText(this, R.string.child_selection_pick_child, Toast.LENGTH_SHORT).show();
            return;
        }

        ChildItem childItem = childItems.get(selectedIndex);
        saveSession(parentId, childItem.id);
        openChildDashboard(childItem.id);
    }

    // פותח ChildDashboardActivity עם extras מפורשים של parentId ו-childId
    private void openChildDashboard(String selectedChildId) {
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        // parentId אומר לדשבורד באיזה ענף /parents להשתמש
        intent.putExtra(KEY_PARENT, parentId);
        // childId אומר לדשבורד איזה ילד לפתוח תחת ההורה
        intent.putExtra(KEY_CHILD, selectedChildId);
        startActivity(intent);
        finish();
    }

    // שומר את בחירת הילד ב-SharedPreferences לכניסה מהירה בפעם הבאה
    private void saveSession(String parentId, String childId) {
        SharedPreferences preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_PARENT, parentId);
        editor.putString(KEY_CHILD, childId);
        editor.apply();
    }

    // בודק null או מחרוזת ריקה אחרי trim
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // מעדכן את כותרת המשנה לפי השאלה האם ההורה כבר ידוע
    private void updateSubtitle() {
        if (tvSubtitle == null) {
            return;
        }

        int subtitleRes;
        if (isBlank(parentId)) {
            subtitleRes = R.string.child_selection_subtitle_parent_unknown;
        } else {
            subtitleRes = R.string.child_selection_subtitle_parent_known;
        }
        tvSubtitle.setText(subtitleRes);
    }

    /**
     * פריט הורה פשוט לספינר ההורים.
     */
    private static class ParentItem {
        private final String id;
        private final String name;

        // שומר מזהה ושם תצוגה של הורה אחד
        private ParentItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * פריט ילד פשוט לספינר הילדים.
     */
    private static class ChildItem {
        private final String id;
        private final String name;

        // שומר מזהה ושם תצוגה של ילד אחד
        private ChildItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
