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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך בחירת ילד — הילד בוחר את ההורה שלו ואז את השם שלו.
 *
 * אם ה-parentId הגיע מה-QR או מסשן שמור — מדלגים על בחירת הורה
 * ומציגים רק את רשימת הילדים של אותו הורה.
 *
 * בלחיצה על "כניסה" — שומר סשן ופותח את ChildDashboardActivity.
 */
public class ChildSelectionActivity extends AppCompatActivity {

    private static final String PREFS = "child_session";
    private static final String KEY_PARENT = "parentId";
    private static final String KEY_CHILD = "childId";

    private TextView tvParentLabel;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);

        bindViews();
        resolveIds();
        btnEnter.setOnClickListener(v -> onEnterClicked());

        if (isBlank(parentId)) {
            showParentPicker();
            loadParents();
        } else {
            hideParentPicker();
            loadChildren(parentId);
        }
    }

    private void bindViews() {
        tvParentLabel = findViewById(R.id.tvParentLabel);
        spinnerParents = findViewById(R.id.spinnerParents);
        tvChildLabel = findViewById(R.id.tvChildLabel);
        spinnerChildren = findViewById(R.id.spinnerChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        btnEnter = findViewById(R.id.btnEnter);
        progressBar = findViewById(R.id.progressBar);
    }

    private void resolveIds() {
        Intent intent = getIntent();
        if (intent != null) {
            parentId = intent.getStringExtra(KEY_PARENT);
            preselectedChildId = intent.getStringExtra(KEY_CHILD);
        }

        if (!isBlank(parentId)) {
            return;
        }

        // אם לא הגענו מ-QR עכשיו, מנסים להמשיך מהסשן האחרון של הילד
        SharedPreferences preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        parentId = preferences.getString(KEY_PARENT, null);
        preselectedChildId = preferences.getString(KEY_CHILD, null);
    }

    private void showParentPicker() {
        tvParentLabel.setVisibility(View.VISIBLE);
        spinnerParents.setVisibility(View.VISIBLE);
        tvChildLabel.setVisibility(View.GONE);
        spinnerChildren.setVisibility(View.GONE);
        btnEnter.setEnabled(false);
    }

    private void hideParentPicker() {
        tvParentLabel.setVisibility(View.GONE);
        spinnerParents.setVisibility(View.GONE);
        tvChildLabel.setVisibility(View.VISIBLE);
        spinnerChildren.setVisibility(View.VISIBLE);
    }

    private void loadParents() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        parentItems.clear();

                        for (DataSnapshot parentSnapshot : snapshot.getChildren()) {
                            String uid = parentSnapshot.getKey();
                            if (isBlank(uid)) {
                                continue;
                            }

                            String firstName = parentSnapshot.child("firstName").getValue(String.class);
                            String lastName = parentSnapshot.child("lastName").getValue(String.class);
                            String fallback = getString(R.string.default_parent_name) + " (" + uid.substring(0, Math.min(uid.length(), 6)) + ")";
                            String fullName = NameUtils.fullNameOrDefault(firstName, lastName, fallback);
                            parentItems.add(new ParentItem(uid, fullName));
                        }

                        if (parentItems.isEmpty()) {
                            Toast.makeText(
                                    ChildSelectionActivity.this,
                                    R.string.child_selection_no_parents,
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        populateParentSpinner();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(
                                ChildSelectionActivity.this,
                                getString(R.string.child_selection_error_loading_parents, error.getMessage()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

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
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= parentItems.size()) {
                    return;
                }

                parentId = parentItems.get(position).id;
                tvChildLabel.setVisibility(View.VISIBLE);
                spinnerChildren.setVisibility(View.VISIBLE);
                loadChildren(parentId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                btnEnter.setEnabled(false);
                tvChildLabel.setVisibility(View.GONE);
                spinnerChildren.setVisibility(View.GONE);
            }
        });
    }

    private void loadChildren(String selectedParentId) {
        progressBar.setVisibility(View.VISIBLE);
        btnEnter.setEnabled(false);

        FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(selectedParentId)
                .child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        childItems.clear();

                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            String childId = childSnapshot.getKey();
                            if (isBlank(childId)) {
                                continue;
                            }

                            String firstName = childSnapshot.child("firstName").getValue(String.class);
                            String lastName = childSnapshot.child("lastName").getValue(String.class);
                            String fallback = getString(R.string.default_child_name) + " (" + childId + ")";
                            String fullName = NameUtils.fullNameOrDefault(firstName, lastName, fallback);
                            childItems.add(new ChildItem(childId, fullName));
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
                        populateChildSpinner();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(
                                ChildSelectionActivity.this,
                                getString(R.string.child_selection_error_loading_children, error.getMessage()),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

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
        // שומרים את הבחירה כדי שהילד יוכל לחזור ישירות לדשבורד בפעם הבאה
        saveSession(parentId, childItem.id);

        Intent intent = new Intent(this, ChildDashboardActivity.class);
        // מעבירים extras מפורשים כדי שהדשבורד ידע בדיוק איזה ילד לפתוח
        intent.putExtra(KEY_PARENT, parentId);
        intent.putExtra(KEY_CHILD, childItem.id);
        startActivity(intent);
        finish();
    }

    private void saveSession(String parentId, String childId) {
        SharedPreferences preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit()
                .putString(KEY_PARENT, parentId)
                .putString(KEY_CHILD, childId)
                .apply();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ParentItem {
        private final String id;
        private final String name;

        private ParentItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class ChildItem {
        private final String id;
        private final String name;

        private ChildItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
