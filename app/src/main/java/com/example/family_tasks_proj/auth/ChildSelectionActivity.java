package com.example.family_tasks_proj.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.example.family_tasks_proj.child.ChildDashboardActivity;
import com.example.family_tasks_proj.utils.NameUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/** מסך לבחירת ילד מתוך רשימה. משמש אחרי סריקת QR או בכניסה ידנית של ילד. */
public class ChildSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_PARENT_ID = "parentId";
    public static final String EXTRA_CHILD_ID = "childId";

    private TextView tvParentLabel, tvSubtitle, tvChildLabel, tvNoChildren;
    private Spinner spinnerParents, spinnerChildren;
    private Button btnEnter;

    private final List<ParentItem> parentItems = new ArrayList<>();
    private final List<ChildItem> childItems = new ArrayList<>();
    private String parentId, preselectedChildId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_selection);
        bindViews();
        resolveIds();
        bindActions();
        openCorrectPicker();
    }

    private void bindViews() {
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvParentLabel = findViewById(R.id.tvParentLabel);
        spinnerParents = findViewById(R.id.spinnerParents);
        tvChildLabel = findViewById(R.id.tvChildLabel);
        spinnerChildren = findViewById(R.id.spinnerChildren);
        tvNoChildren = findViewById(R.id.tvNoChildren);
        btnEnter = findViewById(R.id.btnEnter);
    }

    private void bindActions() {
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEnterClicked();
            }
        });
    }

    // מקבל מזהים רק מה-Intent. אין שמירה מקומית.
    private void resolveIds() {
        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        preselectedChildId = getIntent().getStringExtra(EXTRA_CHILD_ID);
    }

    private void openCorrectPicker() {
        if (parentId == null || parentId.isEmpty()) {
            tvParentLabel.setVisibility(View.VISIBLE);
            spinnerParents.setVisibility(View.VISIBLE);
            tvChildLabel.setVisibility(View.GONE);
            spinnerChildren.setVisibility(View.GONE);
            tvSubtitle.setText(R.string.child_selection_subtitle_parent);
            loadParents();
        } else {
            tvParentLabel.setVisibility(View.GONE);
            spinnerParents.setVisibility(View.GONE);
            tvChildLabel.setVisibility(View.VISIBLE);
            spinnerChildren.setVisibility(View.VISIBLE);
            tvSubtitle.setText(R.string.child_selection_subtitle_child);
            loadChildren(parentId);
        }
    }

    // טוען את כל ההורים הרשומים במערכת מתוך ה-Database
    private void loadParents() {
        FirebaseDatabase.getInstance().getReference("parents").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                parentItems.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String parentUid = snap.getKey();
                    String firstName = snap.child("firstName").getValue(String.class);
                    String lastName = snap.child("lastName").getValue(String.class);
                    parentItems.add(new ParentItem(parentUid, NameUtils.fullNameOrDefault(firstName, lastName, getString(R.string.default_parent_name))));
                }
                if (parentItems.isEmpty()) {
                    Toast.makeText(ChildSelectionActivity.this, R.string.child_selection_no_parents, Toast.LENGTH_LONG).show();
                    return;
                }
                List<String> names = new ArrayList<>();
                for (ParentItem item : parentItems) {
                    names.add(item.name);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ChildSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, names);
                spinnerParents.setAdapter(adapter);
                spinnerParents.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        parentId = parentItems.get(position).id;
                        loadChildren(parentId);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildSelectionActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // טוען את הילדים ששייכים להורה שנבחר
    private void loadChildren(String selectedParentId) {
        FirebaseDatabase.getInstance().getReference("parents").child(selectedParentId).child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childItems.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String childId = snap.getKey();
                    String firstName = snap.child("firstName").getValue(String.class);
                    String lastName = snap.child("lastName").getValue(String.class);
                    childItems.add(new ChildItem(childId, NameUtils.fullNameOrDefault(firstName, lastName, getString(R.string.default_child_name_fallback))));
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
                List<String> names = new ArrayList<>();
                for (ChildItem item : childItems) {
                    names.add(item.name);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ChildSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, names);
                spinnerChildren.setAdapter(adapter);
                if (preselectedChildId != null) {
                    for (int i = 0; i < childItems.size(); i++) {
                        if (preselectedChildId.equals(childItems.get(i).id)) {
                            spinnerChildren.setSelection(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChildSelectionActivity.this, getString(R.string.error_load_db, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onEnterClicked() {
        int selectedPosition = spinnerChildren.getSelectedItemPosition();
        if (selectedPosition < 0) {
            Toast.makeText(this, R.string.child_selection_please_select, Toast.LENGTH_SHORT).show();
            return;
        }
        String childId = childItems.get(selectedPosition).id;
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra(ChildDashboardActivity.EXTRA_PARENT_ID, parentId);
        intent.putExtra(ChildDashboardActivity.EXTRA_CHILD_ID, childId);
        startActivity(intent);
        finish();
    }

    private static class ParentItem {
        String id, name;

        ParentItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class ChildItem {
        String id, name;

        ChildItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
