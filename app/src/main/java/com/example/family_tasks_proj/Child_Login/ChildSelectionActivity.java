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

/** מסך לבחירת ילד מתוך רשימה. משמש כאשר הסריקה זיהתה רק את ההורה או בכניסה ידנית. */
public class ChildSelectionActivity extends AppCompatActivity {

    private TextView tvParentLabel, tvSubtitle, tvChildLabel, tvNoChildren;
    private Spinner spinnerParents, spinnerChildren;
    private Button btnEnter;
    private ProgressBar progressBar;

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
        progressBar = findViewById(R.id.progressBar);
    }

    private void bindActions() {
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEnterClicked();
            }
        });
    }

    // משחזר מזהים מה-Intent או מהזיכרון המקומי
    private void resolveIds() {
        parentId = getIntent().getStringExtra("parentId");
        preselectedChildId = getIntent().getStringExtra("childId");
        if (parentId == null) {
            SharedPreferences sp = getSharedPreferences("child_session", Context.MODE_PRIVATE);
            parentId = sp.getString("parentId", null);
            preselectedChildId = sp.getString("childId", null);
        }
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
        progressBar.setVisibility(View.VISIBLE);
        // ניגש לתיקיית "parents" הראשית כדי להציג את כל המשפחות הקיימות
        // זה משמש כניסה ידנית לילד כשאין QR או כשצריך לבחור משפחה.
        FirebaseDatabase.getInstance().getReference("parents").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
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
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // טוען את הילדים השייכים להורה שנבחר מתוך תת-התיקייה שלו
    private void loadChildren(String selectedParentId) {
        progressBar.setVisibility(View.VISIBLE);
        // ניגש לנתיב parents/{parentId}/children כדי למצוא את הילדים של אותה משפחה
        // ה-Spinner מציג שמות, אבל הבחירה נשמרת לפי childId כדי למנוע בלבול בין שמות דומים.
        FirebaseDatabase.getInstance().getReference("parents").child(selectedParentId).child("children").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
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
                progressBar.setVisibility(View.GONE);
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
        // אחרי בחירת ילד שומרים session מקומי, כדי שבפעם הבאה אפשר יהיה להמשיך מהר יותר.
        SharedPreferences.Editor editor = getSharedPreferences("child_session", MODE_PRIVATE).edit();
        editor.putString("parentId", parentId);
        editor.putString("childId", childId);
        editor.apply();
        Intent intent = new Intent(this, ChildDashboardActivity.class);
        intent.putExtra("parentId", parentId);
        intent.putExtra("childId", childId);
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
