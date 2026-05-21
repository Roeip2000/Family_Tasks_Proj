package com.example.family_tasks_proj.parent;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.family_tasks_proj.R;
import com.google.android.gms.tasks.OnSuccessListener;
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
    private Button btnAdd, btnBack;
    private ListView lvChildren;
    private TextView tvTitle;

    // רשימות למזהי הילדים ולשמות שלהם
    private final List<String> childIds = new ArrayList<>();
    private final List<String> childNames = new ArrayList<>();

    private ArrayAdapter<String> listAdapter;
    private String parentId;
    private String editingChildId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_children);

        // מזהה ההורה המחובר, משמש לבניית הנתיב ב-Firebase
        parentId = FirebaseAuth.getInstance().getUid();

        etChildName = findViewById(R.id.etFirstName);
        btnAdd = findViewById(R.id.btnAddChild);
        btnBack = findViewById(R.id.btnBackToDashboard);
        lvChildren = findViewById(R.id.lvChildren);
        tvTitle = findViewById(R.id.tvFormTitle);

        listAdapter = new ArrayAdapter<>(this,
                R.layout.item_manage_child,
                R.id.tvChildFullName,
                childNames
        );

        lvChildren.setAdapter(listAdapter);

        lvChildren.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startEditChild(position);
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChild();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadChildrenFromFirebase();
    }

    // טעינת הילדים של ההורה מ-Firebase
    private void loadChildrenFromFirebase() {
        getChildrenReference().addValueEventListener(new ValueEventListener() {
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

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // שמירת ילד חדש או עדכון קיים
    private void saveChild() {
        String firstName = etChildName.getText().toString().trim();

        if (firstName.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String childId;
        if (editingChildId != null) {
            childId = editingChildId;
        } else {
            childId = getChildrenReference().push().getKey();
        }

        DatabaseReference childReference = getChildrenReference().child(childId);

        childReference.child("firstName").setValue(firstName).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ManageChildrenActivity.this, R.string.toast_child_saved, Toast.LENGTH_SHORT).show();
                clearForm();
            }
        });
    }

    // ניקוי הטופס וחזרה למצב הוספה
    private void clearForm() {
        editingChildId = null;
        etChildName.setText("");
        btnAdd.setText(R.string.btn_add_child);
        tvTitle.setText(R.string.title_add_child);
    }

    // עדכון הרשימה במסך
    private void updateUI() {
        listAdapter.notifyDataSetChanged();
        fitListHeight(lvChildren);
    }

    private static final int DEFAULT_LIST_WIDTH = 500;

    // חישוב גובה ל-ListView כדי שיוצג בתוך ScrollView
    private void fitListHeight(ListView listView) {
        if (listView.getAdapter() == null) {
            return;
        }

        int listWidth = listView.getWidth();
        if (listWidth <= 0) {
            listWidth = DEFAULT_LIST_WIDTH;
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.AT_MOST);

        int totalHeight = 0;
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            View itemView = listView.getAdapter().getView(i, null, listView);
            itemView.measure(widthSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += itemView.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listView.getAdapter().getCount() - 1));
        listView.setLayoutParams(params);
    }

    // טעינת פרטי ילד לטופס לצורך עריכה
    private void startEditChild(int position) {
        editingChildId = childIds.get(position);
        etChildName.setText(childNames.get(position));
        btnAdd.setText(R.string.btn_update);
        tvTitle.setText(R.string.title_edit_child);
    }

    // מחזיר את הנתיב לילדים ב-Firebase
    private DatabaseReference getChildrenReference() {
        return FirebaseDatabase.getInstance()
                .getReference("parents")
                .child(parentId)
                .child("children");
    }
}
